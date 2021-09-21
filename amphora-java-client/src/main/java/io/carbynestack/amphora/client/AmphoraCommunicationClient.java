/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.httpclient.CsHttpClient;
import io.carbynestack.httpclient.CsHttpClientException;
import io.vavr.control.Try;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.*;
import org.apache.http.Header;

/**
 * A REST client managing the communication with one or multiple endpoints.
 *
 * @param <F> Type used to parse the response body in case an error is returned on HTTP requests.
 */
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class AmphoraCommunicationClient<F> {

  /**
   * A REST request configuration containing the target URI and a list of headers that will be
   * attached to the HTTP request.
   */
  @Value(staticConstructor = "of")
  public static class RequestParameters {
    /** Target URI of the HTTP request. */
    URI uri;
    /** A list of headers that will be attached to the request. */
    List<Header> headers;
  }

  /**
   * A REST request configuration containing the target URI, a list of headers and a message body
   * that will be attached to the HTTP request.
   *
   * @param <T>
   */
  @Value(staticConstructor = "of")
  public static class RequestParametersWithBody<T> {
    /** Target URI of the HTTP request. */
    URI uri;
    /** A list of headers that will be attached to the request. */
    List<Header> headers;
    /** The message body that will be send with the request. */
    T body;
  }

  /** Type used to parse the response body in case an error is returned on HTTP requests. */
  @Getter private final Class<F> failureType;
  /**
   * Defines whether SSL certificate validation is disabled or not.
   *
   * <p><b>WARNING</b><br>
   * Please be aware, that this option leads to insecure web connections and is meant to be used in
   * a local test setup only. Using this option in a productive environment is explicitly <u>not
   * recommended</u>.
   */
  @Getter private final boolean noSslValidation;
  /**
   * The SSL certificates to be trusted on validation check.
   *
   * <p>These certificates are used for validation in addition to the certificates defined in the
   * general truststore.
   */
  @Getter private final List<File> trustedCertificates;

  private final CsHttpClient<F> specsHttpClient;

  /**
   * Creates a new {@link AmphoraCommunicationClient} with the given configuration.
   *
   * <p><b>WARNING</b><br>
   * Please be aware, that disabling the ssl certificate validation leads to insecure web
   * connections and is meant to be used in a local test setup only. Using this option in a
   * productive environment is explicitly <u>not recommended</u>.
   *
   * @param failureType Type used to parse the response body in case an error is returned on HTTP
   *     requests. Must not be <i>null</i>.
   * @param noSslValidation Flag whether to disable the SSL certificate validation or not
   * @param trustedCertificates List of certificates to be trusted on SSL certificate validation.
   *     Must not be <i>null</i>.
   * @param <F> Type of <i>failureType</i>
   * @return A new {@link AmphoraCommunicationClient} according to the given parameters.
   * @throws AmphoraClientException If creating the new {@link AmphoraCommunicationClient} failed.
   * @throws NullPointerException If either failureType or trustedCertificates is <i>null</i>.
   */
  public static <F> AmphoraCommunicationClient<F> of(
      @NonNull Class<F> failureType,
      boolean noSslValidation,
      @NonNull List<File> trustedCertificates)
      throws AmphoraClientException {
    try {
      return new AmphoraCommunicationClient<>(
          failureType,
          noSslValidation,
          trustedCertificates,
          CsHttpClient.<F>builder()
              .withFailureType(failureType)
              .withoutSslValidation(noSslValidation)
              .withTrustedCertificates(trustedCertificates)
              .build());
    } catch (CsHttpClientException sce) {
      throw new AmphoraClientException("Failed to create AmphoraCommunicationClient.", sce);
    }
  }

  /**
   * Sends a HTTP GET request to the endpoint according to the given configuration. The body of the
   * received response will be be parsed to the defined type.
   *
   * @param requestParameters Request configuration containing the target URI and message headers
   * @param responseType Expected response type used to parse the response body.
   * @param <T> Type of the expected response object.
   * @return A {@link Try} either containing the successfully parsed response or an exception if the
   *     request or parsing failed.
   */
  <T> Try<T> download(RequestParameters requestParameters, Class<T> responseType) {
    return Try.of(
        () ->
            specsHttpClient.getForObject(
                requestParameters.getUri(), requestParameters.getHeaders(), responseType));
  }

  /**
   * Sends a HTTP GET request to the endpoints according to the given configurations. The body of
   * the received responses will be be parsed to the defined type.
   *
   * <p>The individual response of a single request configuration is parsed and wrapped in a {@link
   * Try}, either containing the successfully parsed response or an exception if the request or
   * parsing failed. Finally, all results will be wrapped in a {@link Map}, where the URI's of the
   * individual requests are used a the map's keys in order to reference the related responses.
   *
   * @param requestParameters Request configuration containing the target URI and message headers
   * @param responseType Expected response type used to parse the response body.
   * @param <T> Type of the expected response object.
   * @return A {@link Map} containing the response of the individual requests wrapped in a {@link
   *     Try}. A single response can be referenced using the target URI of the related request
   *     configuration
   */
  <T> Map<URI, Try<T>> download(List<RequestParameters> requestParameters, Class<T> responseType) {
    return requestParameters.parallelStream()
        .collect(
            Collectors.toMap(RequestParameters::getUri, params -> download(params, responseType)));
  }

  /**
   * Sends a HTTP POST request to the endpoint according to the given configuration. The body of the
   * received response will be be parsed to the defined type.
   *
   * @param requestParameters Request configuration containing the target URI, message headers and
   *     body
   * @param responseType Expected response type used to parse the response body.
   * @param <T> Type of the expected response object.
   * @return A {@link Try} either containing the successfully parsed response or an exception if the
   *     request or parsing failed.
   */
  <T, U> Try<T> upload(RequestParametersWithBody<U> requestParameters, Class<T> responseType) {
    return Try.of(
        () ->
            specsHttpClient.postForObject(
                requestParameters.getUri(),
                requestParameters.getHeaders(),
                requestParameters.getBody(),
                responseType));
  }

  /**
   * Sends a HTTP POST request to the endpoints according to the given configurations. The body of
   * the received responses will be be parsed to the defined type.
   *
   * <p>The individual response of a single request configuration is parsed and wrapped in a {@link
   * Try}, either containing the successfully parsed response or an exception if the request or
   * parsing failed. Finally, all results will be wrapped in a {@link Map}, where the URI's of the
   * individual requests are used a the map's keys in order to reference the related responses.
   *
   * @param requestParameters Request configuration containing the target URI, message headers and
   *     body
   * @param responseType Expected response type used to parse the response body.
   * @param <T> Type of the expected response object.
   * @return A {@link Map} containing the response of the individual requests wrapped in a {@link
   *     Try}. A single response can be referenced using the target URI of the related request
   *     configuration.
   */
  <T, U> Map<URI, Try<T>> upload(
      List<RequestParametersWithBody<U>> requestParameters, Class<T> responseType) {
    return requestParameters.parallelStream()
        .collect(
            Collectors.toMap(
                RequestParametersWithBody::getUri, params -> upload(params, responseType)));
  }

  /**
   * Sends a HTTP PUT request to the endpoints according to the given configurations.
   *
   * <p>The individual status of a single request is wrapped in a {@link Try}, either being
   * successful or a failure in case an exception occurred or the response returned unsuccessfully.
   *
   * @param requestParameters Request configuration containing the target URI, message headers and
   *     body
   * @return A {@link List} containing the responses of the individual requests wrapped in a {@link
   *     Try}.
   */
  <U> List<Try<Void>> update(List<RequestParametersWithBody<U>> requestParameters) {
    return requestParameters.parallelStream()
        .map(
            params ->
                Try.run(
                    () ->
                        specsHttpClient.put(
                            params.getUri(), params.getHeaders(), params.getBody())))
        .collect(Collectors.toList());
  }

  /**
   * Sends a HTTP DELETE request to the endpoints according to the given configurations.
   *
   * <p>The individual status of a single request is wrapped in a {@link Try}, either being
   * successful or a failure in case an exception occurred or the response returned unsuccessfully.
   *
   * @param requestParameters Request configuration containing the target URI and message headers
   * @return A {@link List} containing the responses of the individual requests wrapped in a {@link
   *     Try}.
   */
  List<Try<Void>> delete(List<RequestParameters> requestParameters) {
    return requestParameters.parallelStream()
        .map(params -> Try.run(() -> specsHttpClient.delete(params.getUri(), params.getHeaders())))
        .collect(Collectors.toList());
  }
}
