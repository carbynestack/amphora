/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints;
import io.vavr.control.Try;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.http.client.utils.URIBuilder;

/**
 * A class, that manages an Amphora Service's URI and provides correct paths for all endpoints. Uses
 * {@link URI} internally.
 */
@Data
@Setter(AccessLevel.NONE)
public class AmphoraServiceUri {

  public static final String INVALID_SERVICE_ADDRESS_EXCEPTION_MSG =
      "Invalid service address.\n"
          + "Address must match the following examples:\n"
          + "\t1. https://server:port\n"
          + "\t2. https://server:port/path\n"
          + "\t3. https://server\n"
          + "\t4. https://server/path\n";
  private final URI serviceUri;
  private final URI secretShareUri;
  private final URI s2sSecretShareUri;
  /** The inter-vc URI to open interim values for multiplications. */
  private final URI interVcOpenInterimValuesUri;

  private final URI maskedInputUri;
  private final URI inputMaskUri;

  /**
   * e.printStackTrace(); Constructs a new <code>AmphoraServiceUri</code> with the given address.
   *
   * @param serviceAddress The base URI of an Amphora Service. Will be parsed to <code>java.net.URI
   *     </code>.
   */
  public AmphoraServiceUri(String serviceAddress) {
    if (serviceAddress == null || serviceAddress.isEmpty()) {
      throw new IllegalArgumentException("serviceAddress must not be empty!");
    }
    try {
      serviceUri = new URI(serviceAddress);
      if (serviceUri.getHost() == null) {
        throw new IllegalArgumentException(INVALID_SERVICE_ADDRESS_EXCEPTION_MSG);
      }
      URI s2sServiceUri =
          composeUri(serviceUri, AmphoraRestApiEndpoints.INTRA_VCP_OPERATIONS_SEGMENT);
      secretShareUri = composeUri(serviceUri, AmphoraRestApiEndpoints.SECRET_SHARES_ENDPOINT);
      s2sSecretShareUri = composeUri(s2sServiceUri, AmphoraRestApiEndpoints.SECRET_SHARES_ENDPOINT);
      interVcOpenInterimValuesUri =
          composeUri(
              serviceUri,
              AmphoraRestApiEndpoints.INTER_VCP_OPERATIONS_SEGMENT
                  + AmphoraRestApiEndpoints.OPEN_INTERIM_VALUES_ENDPOINT);
      maskedInputUri =
          composeUri(serviceUri, AmphoraRestApiEndpoints.UPLOAD_MASKED_INPUTS_ENDPOINT);
      inputMaskUri = composeUri(serviceUri, AmphoraRestApiEndpoints.DOWNLOAD_INPUT_MASKS_ENDPOINT);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to construct AmphoraServiceUri", e);
    }
  }

  private URI composeUri(URI inputUri, String path) throws URISyntaxException {
    String baseUri = inputUri.toString();
    baseUri = baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri;
    return new URI(String.format("%s%s", baseUri, path));
  }

  /**
   * Gets the service-2-service URI of a secret share resource in Amphora.
   *
   * @param secretId The id of the secret share resource
   * @return Secret share resource URI
   */
  public URI getS2SSecretShareResourceUri(UUID secretId) {
    return attachPathParameter(getS2sSecretShareUri(), secretId.toString());
  }

  /**
   * Gets the URI of a secret share resource in Amphora.
   *
   * @param secretId The id of the secret share resource
   * @return Secret share resource URI
   */
  public URI getSecretShareResourceUri(UUID secretId) {
    return attachPathParameter(getSecretShareUri(), secretId.toString());
  }

  /**
   * Gets the URI of the tags endpoint of a secret share resource in Amphora.
   *
   * @param secretId The id of the secret share resource
   * @return Tags endpoint of a secret share resource
   */
  public URI getSecretShareResourceTagsUri(UUID secretId) {
    return attachPathParameter(
        getSecretShareUri(),
        String.format("%s/%s", secretId.toString(), AmphoraRestApiEndpoints.TAGS_ENDPOINT));
  }

  /**
   * Gets the URI of a tag resource of a secret share resource in Amphora.
   *
   * @param secretId The id of the secret share resource
   * @param tagKey The key of the tag resource
   * @return Tag resource of a secret share resource
   */
  public URI getSecretShareResourceUriTagResource(UUID secretId, String tagKey) {
    return attachPathParameter(
        getSecretShareUri(),
        String.format(
            "%s/%s/%s", secretId.toString(), AmphoraRestApiEndpoints.TAGS_ENDPOINT, tagKey));
  }

  URI attachPathParameter(URI uri, String param) {
    URIBuilder uriBuilder = new URIBuilder(uri);
    String path = uriBuilder.getPath();
    path =
        String.format(
            "%s%s%s",
            path == null || path.length() == 0 ? "" : path,
            path != null && path.length() > 0 && path.lastIndexOf('/') == path.length() ? "" : "/",
            param);
    uriBuilder.setPath(path);
    return Try.of(uriBuilder::build)
        .getOrElseThrow(
            throwable ->
                new AmphoraServiceException(
                    String.format(
                        "Failed to compose AmphoraServiceUri for endpoint \"%s\" and param: %s",
                        uri, param),
                    throwable));
  }
}
