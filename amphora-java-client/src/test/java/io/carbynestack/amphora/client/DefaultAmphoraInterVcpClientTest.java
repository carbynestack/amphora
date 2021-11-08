/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.amphora.common.FactorPair;
import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.httpclient.CsHttpClientException;
import io.vavr.control.Try;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAmphoraInterVcpClientTest {
  private final String testUrl1 = "https://amphora.carbynestack.io:8080";
  private final String testUrl2 = "https://amphora.carbynestack.io:8081";
  private final UUID testOperationId = UUID.fromString("72753ecc-464f-42c5-954c-9efa7a5b886c");

  @Mock private AmphoraCommunicationClient<String> amphoraCommunicationClient;

  private DefaultAmphoraInterVcpClient amphoraInterVcpClient;

  private final List<AmphoraServiceUri> amphoraServiceUris =
      Arrays.asList(new AmphoraServiceUri(testUrl1), new AmphoraServiceUri(testUrl2));

  @BeforeEach
  public void setUp() {
    this.amphoraInterVcpClient =
        new DefaultAmphoraInterVcpClient(amphoraServiceUris, amphoraCommunicationClient);
  }

  @Test
  void givenNoServiceUriDefined_whenBuildingInterVcClient_thenThrowException() {
    try (MockedStatic<AmphoraCommunicationClient> communicationClientMockedStatic =
        mockStatic(AmphoraCommunicationClient.class)) {
      DefaultAmphoraInterVcpClient.DefaultAmphoraInterVcpClientBuilder clientBuilder =
          DefaultAmphoraInterVcpClient.Builder();
      IllegalArgumentException actualIae =
          assertThrows(IllegalArgumentException.class, clientBuilder::build);
      assertEquals("At least one amphora service URI needs to be defined.", actualIae.getMessage());
    }
  }

  @Test
  void givenValidExchangeObject_whenOpeningValues_thenSucceed() throws AmphoraClientException {
    MultiplicationExchangeObject exchangeObject = getMultiplicationExchangeObject(testOperationId);
    List<AmphoraCommunicationClient.RequestParametersWithBody<MultiplicationExchangeObject>>
        params =
            amphoraServiceUris.stream()
                .map(
                    uri ->
                        AmphoraCommunicationClient.RequestParametersWithBody.of(
                            uri.getInterVcOpenInterimValuesUri(),
                            ImmutableList.of(),
                            exchangeObject))
                .collect(Collectors.toList());
    Map<URI, Try<Void>> expectedResponse =
        amphoraServiceUris.stream()
            .collect(Collectors.toMap(AmphoraServiceUri::getServiceUri, uri -> Try.success(null)));
    when(amphoraCommunicationClient.upload(params, Void.class)).thenReturn(expectedResponse);
    amphoraInterVcpClient.open(exchangeObject);
    verify(amphoraCommunicationClient, times(1)).upload(params, Void.class);
  }

  @Test
  void givenOnePlayerCannotBeReached_whenOpeningValues_thenThrowException() {
    MultiplicationExchangeObject exchangeObject = getMultiplicationExchangeObject(testOperationId);
    List<AmphoraCommunicationClient.RequestParametersWithBody<MultiplicationExchangeObject>>
        params =
            amphoraServiceUris.stream()
                .map(
                    uri ->
                        AmphoraCommunicationClient.RequestParametersWithBody.of(
                            uri.getInterVcOpenInterimValuesUri(),
                            ImmutableList.of(),
                            exchangeObject))
                .collect(Collectors.toList());
    Map<URI, Try<Void>> expectedResponse =
        amphoraServiceUris.stream()
            .collect(Collectors.toMap(AmphoraServiceUri::getServiceUri, uri -> Try.success(null)));
    expectedResponse.put(
        params.get(params.size() - 1).getUri(), Try.failure(new Exception("Request failed")));
    when(amphoraCommunicationClient.upload(params, Void.class)).thenReturn(expectedResponse);
    assertThrows(AmphoraClientException.class, () -> amphoraInterVcpClient.open(exchangeObject));
  }

  @Test
  void givenOnePlayerRespondsUnsuccessful_whenOpeningValues_thenThrowException() {
    MultiplicationExchangeObject exchangeObject = getMultiplicationExchangeObject(testOperationId);
    List<AmphoraCommunicationClient.RequestParametersWithBody<MultiplicationExchangeObject>>
        params =
            amphoraServiceUris.stream()
                .map(
                    uri ->
                        AmphoraCommunicationClient.RequestParametersWithBody.of(
                            uri.getInterVcOpenInterimValuesUri(),
                            ImmutableList.of(),
                            exchangeObject))
                .collect(Collectors.toList());
    Map<URI, Try<Void>> expectedResponse =
        amphoraServiceUris.stream()
            .collect(Collectors.toMap(AmphoraServiceUri::getServiceUri, uri -> Try.success(null)));
    expectedResponse.put(
        params.get(params.size() - 1).getUri(), Try.failure(new CsHttpClientException("Failure")));
    when(amphoraCommunicationClient.upload(params, Void.class)).thenReturn(expectedResponse);
    assertThrows(AmphoraClientException.class, () -> amphoraInterVcpClient.open(exchangeObject));
  }

  @Test
  void givenMissingOperationId_whenOpeningValues_thenThrowException() {
    MultiplicationExchangeObject exchangeObject = getMultiplicationExchangeObject(null);
    NullPointerException npe =
        assertThrows(NullPointerException.class, () -> amphoraInterVcpClient.open(exchangeObject));
    assertThat(npe.getMessage(), CoreMatchers.containsString("OperationId must not be null"));
    verify(amphoraCommunicationClient, never()).upload(anyList(), any());
  }

  private MultiplicationExchangeObject getMultiplicationExchangeObject(UUID secretId) {
    return new MultiplicationExchangeObject(
        secretId, 0, Collections.singletonList(FactorPair.of(BigInteger.ONE, BigInteger.TEN)));
  }
}
