/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.carbynestack.httpclient.CsHttpClient;
import io.carbynestack.httpclient.CsHttpClientException;
import io.vavr.control.Try;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmphoraCommunicationClientTest {

  private final String testExceptionMessage = "Totally expected :O";
  private final URI testUri = new URI("https://amphora.carbynestack.io:8080");
  private final URI testUri2 = new URI("https://amphora.carbynestack.io:8081");
  private final String response1 = "response1";
  private final String body1 = "body1";
  private final String body2 = "body2";

  private CsHttpClient<String> specsHttpClient;

  private AmphoraCommunicationClient<String> amphoraCommunicationClient;

  public AmphoraCommunicationClientTest() throws URISyntaxException {}

  @SneakyThrows
  @BeforeEach
  public void setUp() {
    try (MockedConstruction<CsHttpClient> httpClientMockedConstruction =
        mockConstruction(CsHttpClient.class)) {
      amphoraCommunicationClient =
          AmphoraCommunicationClient.of(String.class, false, Collections.emptyList());
      specsHttpClient = httpClientMockedConstruction.constructed().get(0);
    }
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenFetchingDataFromOnePlayer_thenReturnExpectedContent() {
    when(specsHttpClient.getForObject(testUri, Collections.emptyList(), String.class))
        .thenReturn(response1);
    String result =
        amphoraCommunicationClient
            .download(
                AmphoraCommunicationClient.RequestParameters.of(testUri, ImmutableList.of()),
                String.class)
            .get();
    assertEquals(response1, result);
  }

  @SneakyThrows
  @Test
  void givenHttpClientThrowsException_whenFetchingDataFromOnePlayer_thenForwardException() {
    CsHttpClientException expectedException = new CsHttpClientException(testExceptionMessage);
    when(specsHttpClient.getForObject(testUri, Collections.emptyList(), String.class))
        .thenThrow(expectedException);
    CsHttpClientException sce =
        assertThrows(
            CsHttpClientException.class,
            () ->
                amphoraCommunicationClient
                    .download(
                        AmphoraCommunicationClient.RequestParameters.of(
                            testUri, ImmutableList.of()),
                        String.class)
                    .get());
    assertEquals(expectedException, sce);
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenFetchingDataFromMultiplePlayers_thenReturnExpectedContent() {
    int numberOfProviders = 2;
    when(specsHttpClient.getForObject(testUri, Collections.emptyList(), String.class))
        .thenReturn(response1);
    String response2 = "response2";
    when(specsHttpClient.getForObject(testUri2, Collections.emptyList(), String.class))
        .thenReturn(response2);
    List<AmphoraCommunicationClient.RequestParameters> params = new ArrayList<>();
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri, ImmutableList.of()));
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri2, ImmutableList.of()));
    Map<URI, Try<String>> result = amphoraCommunicationClient.download(params, String.class);
    assertEquals(numberOfProviders, result.size());
    assertEquals(Try.success(response1), result.get(testUri));
    assertEquals(Try.success(response2), result.get(testUri2));
  }

  @SneakyThrows
  @Test
  void givenOneRequestFails_whenFetchingDataFromMultiplePlayers_thenReturnExpectedResultMap() {
    when(specsHttpClient.getForObject(testUri, Collections.emptyList(), String.class))
        .thenReturn(response1);
    when(specsHttpClient.getForObject(testUri2, Collections.emptyList(), String.class))
        .thenThrow(new CsHttpClientException(testExceptionMessage));
    List<AmphoraCommunicationClient.RequestParameters> params = new ArrayList<>();
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri, ImmutableList.of()));
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri2, ImmutableList.of()));
    Map<URI, Try<String>> result = amphoraCommunicationClient.download(params, String.class);
    assertThat(
            result.entrySet().parallelStream()
                .filter(e -> e.getValue().isSuccess())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()))
        .containsOnly(testUri);
    assertThat(
            result.entrySet().parallelStream()
                .filter(e -> e.getValue().isFailure())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()))
        .containsOnly(testUri2);
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenUploadingToOnePlayer_thenReturnExpectedContent() {
    when(specsHttpClient.postForObject(eq(testUri), anyList(), eq(body1), eq(String.class)))
        .thenReturn(response1);
    String result =
        amphoraCommunicationClient
            .upload(
                AmphoraCommunicationClient.RequestParametersWithBody.of(
                    testUri, ImmutableList.of(), body1),
                String.class)
            .get();
    assertEquals(response1, result);
  }

  @SneakyThrows
  @Test
  void givenHttpClientThrowsException_whenUploadingToOnePlayer_thenForwardException() {
    CsHttpClientException expectedException = new CsHttpClientException(testExceptionMessage);
    when(specsHttpClient.postForObject(eq(testUri), any(List.class), eq(body1), eq(String.class)))
        .thenThrow(expectedException);
    CsHttpClientException sce =
        assertThrows(
            CsHttpClientException.class,
            () ->
                amphoraCommunicationClient
                    .upload(
                        AmphoraCommunicationClient.RequestParametersWithBody.of(
                            testUri, ImmutableList.of(), body1),
                        String.class)
                    .get());
    assertEquals(expectedException, sce);
  }

  @SneakyThrows
  @Test
  void givenOneRequestFails_whenUploadingToMultiplePlayers_thenReturnExpectedResultMap() {
    int numberOfProviders = 2;
    CsHttpClientException expectedException = new CsHttpClientException(testExceptionMessage);
    List<AmphoraCommunicationClient.RequestParametersWithBody<String>> params =
        Lists.newArrayList();
    params.add(
        AmphoraCommunicationClient.RequestParametersWithBody.of(
            testUri, ImmutableList.of(), body1));
    params.add(
        AmphoraCommunicationClient.RequestParametersWithBody.of(
            testUri2, ImmutableList.of(), body2));
    when(specsHttpClient.postForObject(eq(testUri), anyList(), eq(body1), eq(String.class)))
        .thenReturn(null);
    when(specsHttpClient.postForObject(eq(testUri2), anyList(), eq(body2), eq(String.class)))
        .thenThrow(expectedException);
    Map<URI, Try<String>> result = amphoraCommunicationClient.upload(params, String.class);
    assertEquals(numberOfProviders, result.size());
    assertEquals(Try.success(null), result.get(testUri));
    assertTrue(
        result.get(testUri2).isFailure(), String.format("Try for %s should've failed", testUri2));
    assertEquals(expectedException, result.get(testUri2).getCause());
  }

  @SneakyThrows
  @Test
  void givenOneRequestFails_whenUpdatingOnMultiplePlayers_thenReturnExpectedResultMap() {
    CsHttpClientException expectedException = new CsHttpClientException(testExceptionMessage);
    List<AmphoraCommunicationClient.RequestParametersWithBody<String>> params =
        Lists.newArrayList();
    params.add(
        AmphoraCommunicationClient.RequestParametersWithBody.of(
            testUri, ImmutableList.of(), body1));
    params.add(
        AmphoraCommunicationClient.RequestParametersWithBody.of(
            testUri2, ImmutableList.of(), body2));
    doThrow(expectedException).when(specsHttpClient).put(testUri2, Collections.emptyList(), body2);
    List<Try<Void>> updateResults = amphoraCommunicationClient.update(params);
    assertEquals(params.size(), updateResults.size());
    List<Try<Void>> failedResults =
        updateResults.stream().filter(Try::isFailure).collect(Collectors.toList());
    assertEquals(1, failedResults.size(), "Exactly one request should've failed");
    assertEquals(expectedException, failedResults.get(0).getCause());
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenDeletingOnOnePlayer_thenSucceed() {
    amphoraCommunicationClient.delete(
        Lists.newArrayList(
            AmphoraCommunicationClient.RequestParameters.of(testUri, ImmutableList.of())));
    verify(specsHttpClient, times(1)).delete(testUri, Collections.emptyList());
    verifyNoMoreInteractions(specsHttpClient);
  }

  @SneakyThrows
  @Test
  void givenRequestFails_whenDeletingOnOnePlayer_thenForwardException() {
    CsHttpClientException expectedException = new CsHttpClientException(testExceptionMessage);
    doThrow(expectedException).when(specsHttpClient).delete(testUri, Collections.emptyList());
    List<Try<Void>> deleteRequest =
        amphoraCommunicationClient.delete(
            Lists.newArrayList(
                AmphoraCommunicationClient.RequestParameters.of(testUri, ImmutableList.of())));
    assertTrue(deleteRequest.get(0).isFailure(), "Request should've failed");
    assertEquals(expectedException, deleteRequest.get(0).getCause());
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenDeletingFromMultiplePlayer_thenSucceed() {
    List<AmphoraCommunicationClient.RequestParameters> params = new ArrayList<>();
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri, ImmutableList.of()));
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri2, ImmutableList.of()));
    List<Try<Void>> actualResult = amphoraCommunicationClient.delete(params);
    verify(specsHttpClient, times(1)).delete(testUri, Collections.emptyList());
    verify(specsHttpClient, times(1)).delete(testUri2, Collections.emptyList());
    verifyNoMoreInteractions(specsHttpClient);
    actualResult.forEach(t -> assertTrue(t.isSuccess(), "Request should've been successful"));
  }

  @SneakyThrows
  @Test
  void givenOneRequestFails_whenDeletingFromMultiplePlayers_thenReturnExpectedResult() {
    CsHttpClientException expectedException = new CsHttpClientException(testExceptionMessage);
    doThrow(expectedException).when(specsHttpClient).delete(testUri2, Collections.emptyList());
    List<AmphoraCommunicationClient.RequestParameters> params = new ArrayList<>();
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri, ImmutableList.of()));
    params.add(AmphoraCommunicationClient.RequestParameters.of(testUri2, ImmutableList.of()));
    List<Try<Void>> deleteRequests = amphoraCommunicationClient.delete(params);
    assertEquals(params.size(), deleteRequests.size());
    List<Try<Void>> failedResults =
        deleteRequests.stream().filter(Try::isFailure).collect(Collectors.toList());
    assertEquals(1, failedResults.size(), "Exactly one request should've failed");
    assertEquals(expectedException, failedResults.get(0).getCause());
  }
}
