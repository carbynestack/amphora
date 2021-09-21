/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.vavr.control.Try;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/** Client for all Service-to-Service operations. */
@Slf4j
@Setter(value = AccessLevel.NONE)
@Getter(value = AccessLevel.PACKAGE)
@ToString(callSuper = true)
@EqualsAndHashCode
public class DefaultAmphoraInterVcpClient implements AmphoraInterVcpClient {

  private final List<AmphoraServiceUri> serviceUris;
  private final AmphoraCommunicationClient<String> communicationClient;

  /**
   * @param withServiceUris Uris of the Amphora Services of all partners in the VC
   * @param withoutSslValidation Disables the SSL certificate validation check
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
  public DefaultAmphoraInterVcpClient(
      List<AmphoraServiceUri> withServiceUris,
      boolean withoutSslValidation,
      List<File> withTrustedCertificates)
      throws AmphoraClientException {
    this(
        withServiceUris,
        AmphoraCommunicationClient.of(String.class, withoutSslValidation, withTrustedCertificates));
  }

  DefaultAmphoraInterVcpClient(
      List<AmphoraServiceUri> serviceUris, AmphoraCommunicationClient<String> communicationClient) {
    this.communicationClient = communicationClient;
    if (serviceUris == null || serviceUris.isEmpty()) {
      throw new IllegalArgumentException("At least one amphora service URI needs to be defined.");
    }
    this.serviceUris = serviceUris;
  }

  @Override
  public void open(MultiplicationExchangeObject multiplicationExchangeObject)
      throws AmphoraClientException {
    Objects.requireNonNull(
        multiplicationExchangeObject.getOperationId(), "OperationId must not be null");
    checkSuccess(
        communicationClient.upload(
            serviceUris.stream()
                .map(
                    uri ->
                        AmphoraCommunicationClient.RequestParametersWithBody.of(
                            uri.getInterVcOpenInterimValuesUri(),
                            Collections.emptyList(),
                            multiplicationExchangeObject))
                .collect(Collectors.toList()),
            Void.class));
  }

  private void checkSuccess(Map<URI, Try<Void>> uriResponseMap) throws AmphoraClientException {
    List<String> failedRequests =
        uriResponseMap.entrySet().parallelStream()
            .filter(uriTryEntry -> uriTryEntry.getValue().isFailure())
            .map(
                uriTryEntry ->
                    String.format(
                        "Request for endpoint \"%s\" has failed: %s",
                        uriTryEntry.getKey(), uriTryEntry.getValue().getCause()))
            .collect(Collectors.toList());
    if (!failedRequests.isEmpty()) {
      throw new AmphoraClientException(
          String.format(
              "At least one request has failed:%n\t%s",
              failedRequests.parallelStream().collect(Collectors.joining("\n\t"))));
    }
  }
}
