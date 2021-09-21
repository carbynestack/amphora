/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/** Client for all Service-to-Service operations. */
@Slf4j
@Setter(value = AccessLevel.NONE)
@ToString(callSuper = true)
@EqualsAndHashCode
public class DefaultAmphoraIntraVcpClient implements AmphoraIntraVcpClient {
  private final AmphoraServiceUri serviceUri;
  private final AmphoraCommunicationClient<String> communicationClient;

  /**
   * @param withServiceUri Uri of the Amphora Service
   * @param withoutSslValidation Disables the SSL certificate validation check
   *     <p>
   *     <p><b>WARNING</b><br>
   *     Please be aware, that this option leads to insecure web connections and is meant to be used
   *     in a local test setup only. Using this option in a productive environment is explicitly
   *     <u>not recommended</u>.
   * @param withTrustedCertificates List of trusted certificates (.pem) to be added to the trust
   *     store.<br>
   *     This allows tls secured communication with services that do not have a certificate issued
   *     by an official CA (certificate authority).
   * @throws AmphoraClientException If the HTTP(S) client could not be instantiated
   */
  @lombok.Builder(builderMethodName = "Builder")
  public DefaultAmphoraIntraVcpClient(
      AmphoraServiceUri withServiceUri,
      boolean withoutSslValidation,
      List<File> withTrustedCertificates)
      throws AmphoraClientException {
    this(
        withServiceUri,
        AmphoraCommunicationClient.of(String.class, withoutSslValidation, withTrustedCertificates));
  }

  DefaultAmphoraIntraVcpClient(
      AmphoraServiceUri serviceUri, AmphoraCommunicationClient<String> communicationClient) {
    this.communicationClient = communicationClient;
    Objects.requireNonNull(serviceUri, "Service URI must not be null.");
    this.serviceUri = serviceUri;
  }

  @Override
  public UUID uploadSecretShare(SecretShare secretShare) throws AmphoraClientException {
    URI requestUri = serviceUri.getS2sSecretShareUri();
    return communicationClient
        .upload(
            AmphoraCommunicationClient.RequestParametersWithBody.of(
                requestUri, Collections.emptyList(), secretShare),
            URI.class)
        .onFailure(t -> log.error("Creating SecretShare failed", t))
        .map(uri -> UUID.fromString(Paths.get(uri.getPath()).getFileName().toString()))
        .getOrElseThrow(t -> new AmphoraClientException("Creating SecretShare failed", t));
  }

  @Override
  public SecretShare getSecretShare(UUID secretId) throws AmphoraClientException {
    Objects.requireNonNull(secretId, "SecretId must not be null");
    URI requestUri = serviceUri.getS2SSecretShareResourceUri(secretId);
    return communicationClient
        .download(
            AmphoraCommunicationClient.RequestParameters.of(requestUri, Collections.emptyList()),
            SecretShare.class)
        .onFailure(
            t -> log.error(String.format("Fetching secret #%s failed", secretId.toString()), t))
        .getOrElseThrow(
            t ->
                new AmphoraClientException(
                    String.format("Fetching secret #%s failed", secretId.toString()), t));
  }
}
