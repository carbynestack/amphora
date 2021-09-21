/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.*;

import com.google.common.collect.Lists;
import io.carbynestack.amphora.client.AmphoraCommunicationClient.RequestParameters;
import io.carbynestack.amphora.client.AmphoraCommunicationClient.RequestParametersWithBody;
import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.common.exceptions.SecretVerificationException;
import io.carbynestack.amphora.common.paging.PageRequest;
import io.carbynestack.amphora.common.paging.Sort;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import io.carbynestack.httpclient.BearerTokenUtils;
import io.vavr.CheckedFunction1;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * A {@link DefaultAmphoraClient} can be used to communicate with one or many <i>Amphora</i>
 * service(s). It does provide all methods required to exploit the full functionality exposed by the
 * <i>Amphora</i> service(s). Such as create and retrieves secrets, or create and update {@link
 * Tag}s used metadata to describe an {@link Secret}'s content. <br>
 * When uploading data, the secret information is secret shared using an additive secret sharing
 * scheme. Therefore, the generated {@link MaskedInput} is uploaded to all <i>Amphora</i> services
 * defined (see {@link DefaultAmphoraClient#serviceUris}.<br>
 * In case authentication with the server is required (which is <b>highly recommended</b> in a
 * productive environment), bearer tokens can be provided defining a {@link
 * DefaultAmphoraClient#bearerTokenProvider}. The provided tokens will then be used to authenticate
 * with the individual <i>Amphora</i> services.<br>
 * <i>Amphora</i> uses the output delivery protocol (1) to provide secure and confidential access to
 * the individual secret shares. The {@link DefaultAmphoraClient} will reassemble the provided
 * shares and verify the received data accordingly (see {@link SecretShareUtil#verifySecrets(List,
 * List, List, List, List)}.
 *
 * <p>A new {@link DefaultAmphoraClient} can be created using the {@link
 * DefaultAmphoraClient#builder() builder}.
 *
 * <p>(1) Ivan Damgård, Kasper Damgård, Kurt Nielsen, Peter Sebastian Nordholt, Tomas Toft: <br>
 * Confidential Benchmarking based on Multiparty Computation; IACR Cryptology ePrint Archive 2015:
 * 1006 (2015) <br>
 * https://eprint.iacr.org/2015/1006
 */
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode
public class DefaultAmphoraClient implements AmphoraClient {
  /** A pseudorandom number generator used to distribute load on metadata {@code GET} requests. */
  private static final Random RANDOM = new SecureRandom();

  public static final String FETCHING_TAG_WITH_KEY_FOR_SECRET_FAILED_EXCEPTION_MSG =
      "Fetching tag with key \"%s\" for secret #%s failed";
  public static final String FETCHING_TAGS_FOR_SECRET_FAILED_EXCEPTION_MSG =
      "Fetching tags for secret #%s failed";
  public static final String FETCHING_SECRET_METADATA_FAILED_EXCEPTION_MSG =
      "Fetching secret metadata failed";
  public static final String REQUEST_FOR_ENDPOINT_FAILED_EXCEPTION_MSG =
      "Request for endpoint \"%s\" failed: %s";

  final List<AmphoraServiceUri> serviceUris;
  final Option<BearerTokenProvider<AmphoraServiceUri>> bearerTokenProvider;
  final SecretShareUtil secretShareUtil;
  final AmphoraCommunicationClient<String> communicationClient;

  /**
   * Creates a new {@link DefaultAmphoraClient} with the specified {@link
   * DefaultAmphoraClientBuilder} attributes. The client is capable to communicate with the given
   * services using either http or https, according to the scheme defined by the give url. In
   * addition trustworthy SSL certificates can be defined to allow secure communication with
   * services that provide self-signed certificates or ssl certificate validation can be disabled.
   *
   * @param builder An {@link DefaultAmphoraClientBuilder} object containing the client's
   *     configuration.
   * @throws AmphoraClientException If the HTTP(S) client could not be instantiated.
   */
  DefaultAmphoraClient(DefaultAmphoraClientBuilder builder) throws AmphoraClientException {
    this(
        builder,
        AmphoraCommunicationClient.of(
            String.class, builder.noSslValidation(), builder.trustedCertificates()));
  }

  /**
   * Creates a new {@link DefaultAmphoraClient} with the specified {@link
   * DefaultAmphoraClientBuilder} configuration and a given {@link AmphoraCommunicationClient}. The
   * client is capable to communicate with the given services using either http or https, according
   * to the scheme defined by the give url. In addition trustworthy SSL certificates can be defined
   * to allow secure communication with services that provide self-signed certificates or ssl
   * certificate validation can be disabled.
   *
   * @param builder An {@link DefaultAmphoraClientBuilder} secret containing the client's
   *     configuration.
   * @param communicationClient The {@link AmphoraCommunicationClient} used for communication with
   *     the amphora services.
   */
  DefaultAmphoraClient(
      DefaultAmphoraClientBuilder builder, AmphoraCommunicationClient<String> communicationClient) {
    this.communicationClient = communicationClient;
    this.serviceUris = builder.serviceUris();
    this.secretShareUtil = SecretShareUtil.of(builder.prime(), builder.r(), builder.rInv());
    this.bearerTokenProvider = Option.of(builder.bearerTokenProvider());
  }

  private List<Header> getHeaders(AmphoraServiceUri uri) {
    return bearerTokenProvider
        .map(p -> BearerTokenUtils.createBearerToken(p.apply(uri)))
        .toJavaList();
  }

  /**
   * Takes a secret, splits its data into multiple shares and uploads them as secret shares to the
   * amphora services participating in the MPC cluster given by {@link
   * DefaultAmphoraClient#serviceUris}.
   *
   * <p><i>Amphora</i> uses additive secret sharing which allows for compatibility with SPDZ.
   *
   * @param secret The secret to be secret shared and uploaded to the services
   * @return The id of the uploaded secret
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if there already exists a secret f the given id.
   */
  @Override
  public UUID createSecret(Secret secret) throws AmphoraClientException {
    List<TupleList> inputMaskListMap =
        downloadInputMasks(secret.size(), secret.getSecretId().toString());
    TupleList[] inputMaskLists = inputMaskListMap.toArray(new TupleList[0]);
    Map<Integer, BigInteger> zippedInputMasks =
        IntStream.range(0, secret.size())
            .boxed()
            .parallel()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    i ->
                        IntStream.range(0, inputMaskLists.length)
                            .parallel()
                            .mapToObj(
                                j -> ((InputMask) inputMaskLists[j].get(i)).getShare(0).getValue())
                            .collect(secretShareUtil.summingGfpAsBigInteger())));
    List<MaskedInputData> maskedInputData =
        secretShareUtil.zippedMapToOrderedList(
            zippedInputMasks.entrySet().parallelStream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        entry ->
                            secretShareUtil.maskInput(
                                secret.getData()[entry.getKey()], entry.getValue()))));
    MaskedInput maskedInput =
        new MaskedInput(secret.getSecretId(), maskedInputData, secret.getTags());
    List<RequestParametersWithBody<MaskedInput>> params =
        mapServiceUris(
            uri ->
                RequestParametersWithBody.of(
                    uri.getMaskedInputUri(), getHeaders(uri), maskedInput));
    checkSuccess(communicationClient.upload(params, URI.class));
    return secret.getSecretId();
  }

  /**
   * Deletes a secret, or more precisely its secret shares from all the defined {@link
   * DefaultAmphoraClient#serviceUris}.
   *
   * @param secretId The id of the secret to delete
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if no secret with the given id exists.
   */
  @Override
  public void deleteSecret(UUID secretId) throws AmphoraClientException {
    unwrap(
        communicationClient.delete(
            mapServiceUris(
                uri ->
                    RequestParameters.of(
                        uri.getSecretShareResourceUri(secretId), getHeaders(uri)))));
  }

  /**
   * Retrieves the distributed secret by a given id.
   *
   * <p>The {@link DefaultAmphoraClient} will therefore fetch and reassemble all shares for the
   * given {@link DefaultAmphoraClient#serviceUris}. Based on the secure download protocol, the
   * integrity of the received secret is verified.
   *
   * @param secretId The id of the secret
   * @return The requested secret
   * @throws SecretVerificationException if the vefification of the reassembled secret fails. This
   *     could either happen based on errors during transmition or at least one of the parties
   *     behaving dishonest.
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if no secret with the given id exists.
   */
  @Override
  public Secret getSecret(UUID secretId) throws AmphoraClientException {
    List<OutputDeliveryObject> outputObjects = getOutputDeliveryObjects(secretId);
    List<BigInteger> secrets =
        secretShareUtil.recombineObject(
            outputObjects.stream()
                .map(OutputDeliveryObject::getSecretShares)
                .collect(Collectors.toList()));
    List<BigInteger> rs =
        secretShareUtil.recombineObject(
            outputObjects.stream()
                .map(OutputDeliveryObject::getRShares)
                .collect(Collectors.toList()));
    List<BigInteger> us =
        secretShareUtil.recombineObject(
            outputObjects.stream()
                .map(OutputDeliveryObject::getUShares)
                .collect(Collectors.toList()));
    List<BigInteger> vs =
        secretShareUtil.recombineObject(
            outputObjects.stream()
                .map(OutputDeliveryObject::getVShares)
                .collect(Collectors.toList()));
    List<BigInteger> ws =
        secretShareUtil.recombineObject(
            outputObjects.stream()
                .map(OutputDeliveryObject::getWShares)
                .collect(Collectors.toList()));
    secretShareUtil.verifySecrets(secrets, rs, us, vs, ws);
    log.debug("Secret verified.");
    return Secret.of(secretId, outputObjects.get(0).getTags(), secrets.toArray(new BigInteger[0]));
  }

  /**
   * Retrieves a list of all stored secret ids and their respective tags.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @return A list of secret metadata. An empty list if there are no secrets available.
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  @Override
  public List<Metadata> getSecrets() throws AmphoraClientException {
    return getSecrets(new ArrayList<>());
  }

  /**
   * Retrieves a list of all stored secret's metadata with the tags matching the given filter
   * criteria.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param filterCriteria A list of filters on tags to select only a subset of the stored secrets.
   *     If no filtering is desired, an empty list can be passed.
   * @return A list of secret metadata. An empty list if there are no secrets available.
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  @Override
  public List<Metadata> getSecrets(List<TagFilter> filterCriteria) throws AmphoraClientException {
    List<NameValuePair> parameters = addFilters(new ArrayList<>(), filterCriteria);
    MetadataPage metadataPage = getObjectMetadata(parameters);
    return metadataPage.getContent();
  }

  /**
   * Retrieves a page with a list of all stored secret's metadata.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param pageRequest Defines pagination and, if specified, sorting on the result. Only the first
   *     sort order is used.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  @Override
  public MetadataPage getSecrets(PageRequest pageRequest) throws AmphoraClientException {
    return getSecrets(new ArrayList<>(), pageRequest);
  }

  /**
   * Retrieves a page with a list of all stored secret's metadata.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param sort Sort configuration to be applied on the request.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  @Override
  public List<Metadata> getSecrets(Sort sort) throws AmphoraClientException {
    List<NameValuePair> parameters = addSorting(new ArrayList<>(), sort);
    MetadataPage metadataPage = getObjectMetadata(parameters);
    return metadataPage.getContent();
  }

  /**
   * Retrieves a page of a list of all stored secret's metadata with the tags matching the given
   * filter criteria.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param filterCriteria A list of filters on tags to select only a subset of the stored secrets.
   *     If no filtering is desired, an empty list can be passed.
   * @param sort Sort configuration to be applied on the request.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  @Override
  public List<Metadata> getSecrets(List<TagFilter> filterCriteria, Sort sort)
      throws AmphoraClientException {
    List<NameValuePair> parameters = addFilters(new ArrayList<>(), filterCriteria);
    addSorting(parameters, sort);
    MetadataPage metadataPage = getObjectMetadata(parameters);
    return metadataPage.getContent();
  }

  /**
   * Retrieves a page of a list of all stored secret's metadata with the tags matching the given
   * filter criteria.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param filterCriteria A list of filters on tags to select only a subset of the stored secrets.
   *     If no filtering is desired, an empty list can be passed.
   * @param pageRequest Defines pagination and, if specified, sorting on the result. Only the first
   *     sort order is used.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  @Override
  public MetadataPage getSecrets(List<TagFilter> filterCriteria, PageRequest pageRequest)
      throws AmphoraClientException {
    List<NameValuePair> parameters = addFilters(new ArrayList<>(), filterCriteria);
    addPagination(parameters, pageRequest);
    return getObjectMetadata(parameters);
  }

  /**
   * Retrieves all tags associated with a secret stored in Amphora.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param secretId The id of the secret for which the tags will be retrieved
   * @return All tags associated with the secret
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if no secret with the given id exists.
   */
  @Override
  public List<Tag> getTags(UUID secretId) throws AmphoraClientException {
    AmphoraServiceUri target = selectRandomServiceUri(serviceUris);
    URI requestUri = target.getSecretShareResourceTagsUri(secretId);
    Tag[] tags =
        communicationClient
            .download(RequestParameters.of(requestUri, getHeaders(target)), Tag[].class)
            .onFailure(
                t ->
                    log.error(
                        String.format(FETCHING_TAGS_FOR_SECRET_FAILED_EXCEPTION_MSG, secretId), t))
            .getOrElseThrow(
                t ->
                    new AmphoraClientException(
                        String.format(FETCHING_TAGS_FOR_SECRET_FAILED_EXCEPTION_MSG, secretId), t));
    return Arrays.asList(tags);
  }

  @Override
  public void createTag(UUID secretId, Tag tag) throws AmphoraClientException {
    checkSuccess(
        communicationClient.upload(
            mapServiceUris(
                uri ->
                    RequestParametersWithBody.of(
                        uri.getSecretShareResourceTagsUri(secretId), getHeaders(uri), tag)),
            URI.class));
  }

  @Override
  public void overwriteTags(UUID secretId, List<Tag> tags) throws AmphoraClientException {
    unwrap(
        communicationClient.update(
            mapServiceUris(
                uri ->
                    RequestParametersWithBody.of(
                        uri.getSecretShareResourceTagsUri(secretId), getHeaders(uri), tags))));
  }

  /**
   * Retrieves a secret's tag by its key.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param secretId The id of the secret for which the tag will be retrieved
   * @param tagKey The key of the requested tag
   * @return The secret's tag
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed, if no secret with the given id exists or if no tag with the
   *     given key exists.
   */
  @Override
  public Tag getTag(UUID secretId, String tagKey) throws AmphoraClientException {
    AmphoraServiceUri target = selectRandomServiceUri(serviceUris);
    URI requestUri = target.getSecretShareResourceUriTagResource(secretId, tagKey);
    return communicationClient
        .download(RequestParameters.of(requestUri, getHeaders(target)), Tag.class)
        .onFailure(
            t ->
                log.error(
                    String.format(
                        FETCHING_TAG_WITH_KEY_FOR_SECRET_FAILED_EXCEPTION_MSG, tagKey, secretId),
                    t))
        .getOrElseThrow(
            t ->
                new AmphoraClientException(
                    String.format(
                        FETCHING_TAG_WITH_KEY_FOR_SECRET_FAILED_EXCEPTION_MSG, tagKey, secretId),
                    t));
  }

  @Override
  public void updateTag(UUID secretId, Tag tag) throws AmphoraClientException {
    unwrap(
        communicationClient.update(
            mapServiceUris(
                uri ->
                    RequestParametersWithBody.of(
                        uri.getSecretShareResourceUriTagResource(secretId, tag.getKey()),
                        getHeaders(uri),
                        tag))));
  }

  @Override
  public void deleteTag(UUID secretId, String tagKey) throws AmphoraClientException {
    unwrap(
        communicationClient.delete(
            mapServiceUris(
                uri ->
                    RequestParameters.of(
                        uri.getSecretShareResourceUriTagResource(secretId, tagKey),
                        getHeaders(uri)))));
  }

  /**
   * Creates and returns a new {@link DefaultAmphoraClientBuilder}
   *
   * @return a new {@link DefaultAmphoraClientBuilder}
   */
  public static DefaultAmphoraClientBuilder builder() {
    return new DefaultAmphoraClientBuilder();
  }

  /**
   * Retrieves the {@link OutputDeliveryObject}s from all services for a secret with the given ID
   *
   * @param secretId The ID of the secret
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  private List<OutputDeliveryObject> getOutputDeliveryObjects(UUID secretId)
      throws AmphoraClientException {
    UUID requestId = UUID.randomUUID();
    return Lists.newArrayList(
        unwrap(
                communicationClient.download(
                    mapServiceUris(
                        uri ->
                            RequestParameters.of(
                                new URIBuilder(uri.getSecretShareResourceUri(secretId))
                                    .addParameter(REQUEST_ID_PARAMETER, requestId.toString())
                                    .build(),
                                getHeaders(uri))),
                    OutputDeliveryObject.class))
            .values());
  }

  /**
   * Download the metadata for all available secrets matching the given criteria.
   *
   * <p>Since all services are expected to share the exact same metadata, only one of the defined
   * {@link DefaultAmphoraClient#serviceUris} will be queried in order to receive the requested
   * information.
   *
   * @param parameters filter or sorting criteria
   * @return A page containing the available secrets according to the given criteria
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if the given parameters are in an invalid format
   */
  private MetadataPage getObjectMetadata(List<NameValuePair> parameters)
      throws AmphoraClientException {
    AmphoraServiceUri target = serviceUris.get(0);
    try {
      URI requestUri = target.getSecretShareUri();
      if (!parameters.isEmpty()) {
        requestUri = new URIBuilder(requestUri).addParameters(parameters).build();
      }
      return communicationClient
          .download(RequestParameters.of(requestUri, getHeaders(target)), MetadataPage.class)
          .onFailure(t -> log.error(FETCHING_SECRET_METADATA_FAILED_EXCEPTION_MSG, t))
          .getOrElseThrow(
              t -> new AmphoraClientException(FETCHING_SECRET_METADATA_FAILED_EXCEPTION_MSG, t));
    } catch (URISyntaxException use) {
      throw new AmphoraClientException(use.getMessage(), use);
    }
  }

  /**
   * Converts a list of {@link TagFilter} into a set of {@link NameValuePair} and appends it to the
   * given parameter list. The filter criteria are converted (encoded) in such a way to be passed as
   * uri request parameters to a http(s) request.
   *
   * @throws AmphoraClientException if the given criteria cannot be converted
   */
  private List<NameValuePair> addFilters(
      List<NameValuePair> parameters, List<TagFilter> filterCriteria)
      throws AmphoraClientException {
    String filter = "";
    try {
      for (TagFilter criterion : filterCriteria) {
        filter =
            String.format(
                "%s%s%s%s%s",
                filter,
                URLEncoder.encode(criterion.getKey(), StandardCharsets.UTF_8.name()),
                criterion.getOperator(),
                URLEncoder.encode(criterion.getValue(), StandardCharsets.UTF_8.name()),
                CRITERIA_SEPARATOR);
      }
      if (!filter.isEmpty()) {
        parameters.add(
            new BasicNameValuePair(FILTER_PARAMETER, filter.substring(0, filter.length() - 1)));
      }
      return parameters;
    } catch (UnsupportedEncodingException uee) {
      throw new AmphoraClientException(uee.getMessage(), uee);
    }
  }

  private List<NameValuePair> addPagination(
      List<NameValuePair> parameters, PageRequest pageRequest) {
    if (pageRequest != null) {
      parameters.add(
          new BasicNameValuePair(PAGE_NUMBER_PARAMETER, String.valueOf(pageRequest.getPage())));
      parameters.add(
          new BasicNameValuePair(PAGE_SIZE_PARAMETER, String.valueOf(pageRequest.getSize())));
      addSorting(parameters, pageRequest.getSort());
    }
    return parameters;
  }

  private List<NameValuePair> addSorting(List<NameValuePair> parameters, Sort sort) {
    if (sort != null) {
      parameters.add(new BasicNameValuePair(SORT_PROPERTY_PARAMETER, sort.getProperty()));
      parameters.add(new BasicNameValuePair(SORT_DIRECTION_PARAMETER, sort.getOrder().name()));
    }
    return parameters;
  }

  private <T> void checkSuccess(Map<URI, Try<T>> uriResponseMap) throws AmphoraClientException {
    if (log.isDebugEnabled()) {
      uriResponseMap.forEach(
          (key, value) ->
              log.debug(String.format("%s provided the following response: %s", key, value)));
    }
    Map<URI, Throwable> failedRequests =
        uriResponseMap.entrySet().parallelStream()
            .filter(entry -> !entry.getValue().isSuccess())
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getCause()));

    if (!failedRequests.isEmpty()) {
      StringBuilder message =
          new StringBuilder(
              "Secret could not be created due to http errors returned by the following"
                  + " providers: ");
      failedRequests.forEach(
          (k, v) -> {
            log.error(String.format(REQUEST_FOR_ENDPOINT_FAILED_EXCEPTION_MSG, k, v));
            message.append(
                String.format(
                    "%n\t" + REQUEST_FOR_ENDPOINT_FAILED_EXCEPTION_MSG, k, v.getMessage()));
          });
      throw new AmphoraClientException(message.toString());
    }
  }

  private List<TupleList> downloadInputMasks(long count, String requestId)
      throws AmphoraClientException {
    List<NameValuePair> queryParams =
        Lists.newArrayList(
            new BasicNameValuePair(REQUEST_ID_PARAMETER, requestId),
            new BasicNameValuePair(COUNT_PARAMETER, String.valueOf(count)));

    return new ArrayList<>(
        unwrap(
                communicationClient.download(
                    mapServiceUris(
                        uri ->
                            Try.of(
                                    () ->
                                        RequestParameters.of(
                                            new URIBuilder(uri.getInputMaskUri())
                                                .setParameters(queryParams)
                                                .build(),
                                            getHeaders(uri)))
                                .getOrElseThrow(
                                    throwable ->
                                        new AmphoraServiceException(
                                            String.format(
                                                "Failed to construct InputMaskUri for endpoint"
                                                    + " \"%s\"",
                                                uri),
                                            throwable))),
                    TupleList.class))
            .values());
  }

  private AmphoraServiceUri selectRandomServiceUri(List<AmphoraServiceUri> uris) {
    return uris.get(RANDOM.nextInt(uris.size()));
  }

  private <T> List<T> mapServiceUris(CheckedFunction1<AmphoraServiceUri, T> f)
      throws AmphoraClientException {
    return Try.of(
            () ->
                serviceUris.stream()
                    .map(
                        uri ->
                            Try.of(() -> f.apply(uri))
                                .getOrElseThrow(
                                    t ->
                                        new IllegalStateException(
                                            String.format(
                                                "Failed to apply function on URI: %s", uri),
                                            t)))
                    .collect(Collectors.toList()))
        .getOrElseThrow(t -> new AmphoraClientException("Failed to map URIs", t));
  }

  private <T> Map<URI, T> unwrap(Map<URI, Try<T>> tries) throws AmphoraClientException {
    Map<URI, Throwable> failures =
        tries.entrySet().parallelStream()
            .filter(es -> es.getValue().isFailure())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCause()));
    if (!failures.isEmpty()) {
      failures.forEach(
          (k, v) -> log.error(String.format("Request failed for service at URI \"%s\"", k), v));
      throw new AmphoraClientException(
          String.format(
              "Error(s) occurred while processing responses:%n\t%s",
              failures.values().parallelStream()
                  .map(Throwable::getMessage)
                  .collect(Collectors.joining("\n\t"))));
    }
    return tries.entrySet().parallelStream()
        .collect(Collectors.toMap(Map.Entry::getKey, es -> es.getValue().get()));
  }

  private <T> List<T> unwrap(List<Try<T>> tries) throws AmphoraClientException {
    List<Throwable> failures =
        tries.parallelStream()
            .filter(Try::isFailure)
            .map(Try::getCause)
            .collect(Collectors.toList());
    if (!failures.isEmpty()) {
      failures.forEach(t -> log.error("Request has failed", t));
      throw new AmphoraClientException(
          String.format(
              "At least one request has failed:%n\t%s",
              failures.parallelStream()
                  .map(Throwable::getMessage)
                  .collect(Collectors.joining("\n\t"))));
    }
    return tries.stream().map(Try::get).collect(Collectors.toList());
  }
}
