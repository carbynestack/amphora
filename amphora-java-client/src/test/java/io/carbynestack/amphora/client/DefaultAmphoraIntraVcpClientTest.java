/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import static io.carbynestack.amphora.client.TestData.getTags;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.SECRET_SHARES_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import io.carbynestack.amphora.client.AmphoraCommunicationClient.RequestParameters;
import io.carbynestack.amphora.common.AmphoraServiceUri;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.castor.common.exceptions.CastorServiceException;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import io.vavr.control.Try;
import java.math.BigInteger;
import java.net.URI;
import java.util.Random;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAmphoraIntraVcpClientTest {
  private final String testUrl = "https://amphora.carbynestack.io";
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");

  @Mock private AmphoraCommunicationClient amphoraCommunicationClient;

  private DefaultAmphoraIntraVcpClient amphoraIntraVcpClient;

  private final AmphoraServiceUri amphoraServiceUri = new AmphoraServiceUri(testUrl);

  private MpSpdzIntegrationUtils spdzUtil;

  private final Random rnd = new Random(42);

  @BeforeEach
  public void setUp() {
    spdzUtil =
        MpSpdzIntegrationUtils.of(
            new BigInteger("198766463529478683931867765928436695041"),
            new BigInteger("141515903391459779531506841503331516415"),
            new BigInteger("133854242216446749056083838363708373830"));
    this.amphoraIntraVcpClient =
        new DefaultAmphoraIntraVcpClient(amphoraServiceUri, amphoraCommunicationClient);
  }

  @Test
  void givenNoServiceUriDefined_whenBuildingInterVcClient_thenThrowException() {
    try (MockedStatic<AmphoraCommunicationClient> communicationClientMockedStatic =
        mockStatic(AmphoraCommunicationClient.class)) {
      DefaultAmphoraIntraVcpClient.DefaultAmphoraIntraVcpClientBuilder clientBuilder =
          DefaultAmphoraIntraVcpClient.Builder();
      NullPointerException actualNpe =
          assertThrows(NullPointerException.class, clientBuilder::build);
      assertThat(actualNpe.getMessage()).contains("Service URI must not be null");
    }
  }

  @SneakyThrows
  @Test
  void givenShareIsValid_whenUploadSecretShare_thenSucceed() {
    URI expectedUri = URI.create(testUrl + SECRET_SHARES_ENDPOINT + "/" + testSecretId);
    SecretShare secretShare = getSecretShare(testSecretId);
    when(amphoraCommunicationClient.upload(
            AmphoraCommunicationClient.RequestParametersWithBody.of(
                amphoraServiceUri.getS2sSecretShareUri(), ImmutableList.of(), secretShare),
            URI.class))
        .thenReturn(Try.success(expectedUri));
    assertEquals(testSecretId, amphoraIntraVcpClient.uploadSecretShare(secretShare));
  }

  @SneakyThrows
  @Test
  void givenIdIsValid_whenDownloadingSecretShare_thenReturnSecretShare() {
    SecretShare secretShare = getSecretShare(testSecretId);
    when(amphoraCommunicationClient.download(
            RequestParameters.of(
                amphoraServiceUri.getS2SSecretShareResourceUri(testSecretId), ImmutableList.of()),
            SecretShare.class))
        .thenReturn(Try.success(secretShare));
    assertEquals(secretShare, amphoraIntraVcpClient.getSecretShare(testSecretId));
  }

  @Test
  void givenIdIsNull_whenDownloadingSecretShare_thenThrowException() {
    NullPointerException npe =
        assertThrows(NullPointerException.class, () -> amphoraIntraVcpClient.getSecretShare(null));
    assertThat(npe.getMessage()).contains("SecretId must not be null");
    verify(amphoraCommunicationClient, never()).download(any(RequestParameters.class), any());
  }

  @Test
  void givenSecretShareWithIdDoesNotExist_whenDownloadingSecretShare_thenThrowException() {
    when(amphoraCommunicationClient.download(
            RequestParameters.of(
                amphoraServiceUri.getS2SSecretShareResourceUri(testSecretId), ImmutableList.of()),
            SecretShare.class))
        .thenReturn(Try.failure(new CastorServiceException("Secret with ID does not exist.")));
    AmphoraClientException ace =
        assertThrows(
            AmphoraClientException.class, () -> amphoraIntraVcpClient.getSecretShare(testSecretId));
    assertEquals(String.format("Fetching secret #%s failed", testSecretId), ace.getMessage());
  }

  private SecretShare getSecretShare(UUID id) {
    return SecretShare.builder()
        .secretId(id)
        .data(
            ArrayUtils.addAll(
                spdzUtil.toGfp(BigInteger.valueOf(Math.abs(rnd.nextLong()))),
                new byte[MpSpdzIntegrationUtils.WORD_WIDTH]))
        .tags(getTags())
        .build();
  }
}
