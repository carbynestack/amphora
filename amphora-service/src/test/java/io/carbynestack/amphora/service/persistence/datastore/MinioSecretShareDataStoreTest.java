/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.datastore;

import static io.carbynestack.amphora.service.persistence.datastore.MinioSecretShareDataStore.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.AmphoraTestData;
import io.carbynestack.amphora.service.config.MinioProperties;
import io.minio.*;
import io.minio.errors.InternalException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinioSecretShareDataStoreTest {
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");

  @Mock private MinioClient minioClientMock;

  private final String BUCKET = "my-bucket";

  private SecretShareDataStore secretShareDataStore;

  @BeforeEach
  public void setUp() {
    secretShareDataStore =
        new MinioSecretShareDataStore(minioClientMock, new MinioProperties().setBucket(BUCKET));
  }

  @SneakyThrows
  @Test
  void
      givenMakingMinioBucketFails_whenInstantiatingMinioSecretShareDataStore_thenThrowAmphoraServiceException() {
    MinioProperties minioProperties = new MinioProperties().setBucket(BUCKET);
    InternalException expectedCause = new InternalException(BUCKET, "");
    doReturn(false)
        .when(minioClientMock)
        .bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build());
    doThrow(expectedCause)
        .when(minioClientMock)
        .makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () -> new MinioSecretShareDataStore(minioClientMock, minioProperties));
    assertEquals(FAILED_INITIALIZING_EXCEPTION_MSG, ase.getMessage());
    assertEquals(expectedCause, ase.getCause());
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenStoreSecretShareData_thenWriteExpectedDataToMinio() {
    SecretShare secretShare = AmphoraTestData.getRandomSecretShare(testSecretId);
    secretShareDataStore.storeSecretShareData(secretShare.getSecretId(), secretShare.getData());

    ArgumentCaptor<PutObjectArgs> poaCaptor = ArgumentCaptor.forClass(PutObjectArgs.class);
    verify(minioClientMock).putObject(poaCaptor.capture());
    PutObjectArgs actualPoa = poaCaptor.getValue();
    assertEquals(BUCKET, actualPoa.bucket());
    assertEquals(secretShare.getSecretId().toString(), actualPoa.object());
    assertEquals("application/octet-stream", actualPoa.contentType());
    assertArrayEquals(secretShare.getData(), IOUtils.toByteArray(actualPoa.stream()));
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenGetSecretShareData_thenReturnExpectedContent() {
    SecretShare secretShare = AmphoraTestData.getRandomSecretShare(testSecretId);
    doReturn(
            new GetObjectResponse(
                null,
                BUCKET,
                null,
                testSecretId.toString(),
                new ByteArrayInputStream(secretShare.getData())))
        .when(minioClientMock)
        .getObject(
            GetObjectArgs.builder()
                .bucket(BUCKET)
                .object(secretShare.getSecretId().toString())
                .build());

    byte[] actualSecretShareData =
        secretShareDataStore.getSecretShareData(secretShare.getSecretId());
    MatcherAssert.assertThat(actualSecretShareData, Matchers.is(secretShare.getData()));
  }

  @Test
  void givenNoDataAvailableForGivenId_whenGetSecretShareData_thenThrowAmphoraServiceException() {
    UUID unknownSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () -> secretShareDataStore.getSecretShareData(unknownSecretId));
    assertEquals(
        String.format(GET_DATA_FOR_SECRET_EXCEPTION_MSG, unknownSecretId, "inputStream"),
        ase.getMessage());
  }

  @SneakyThrows
  @Test
  void givenMinioThrowsException_whenStoreSecretShareData_thenWrapInAmphoraServiceException() {
    SecretShare secretShare = AmphoraTestData.getRandomSecretShare(testSecretId);
    byte[] shareData = secretShare.getData();
    IOException expectedCause = new IOException("Totally expected that move..");

    doThrow(expectedCause).when(minioClientMock).putObject(any(PutObjectArgs.class));

    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () -> secretShareDataStore.storeSecretShareData(testSecretId, shareData));
    assertEquals(expectedCause.getMessage(), ase.getMessage());
    assertEquals(expectedCause, ase.getCause());
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenDeleteSecretShareData_thenReturnExpectedContent() {
    SecretShare secretShare = AmphoraTestData.getRandomSecretShare(testSecretId);

    doReturn(
            new GetObjectResponse(
                null,
                BUCKET,
                null,
                testSecretId.toString(),
                new ByteArrayInputStream(secretShare.getData())))
        .when(minioClientMock)
        .getObject(GetObjectArgs.builder().bucket(BUCKET).object(testSecretId.toString()).build());

    MatcherAssert.assertThat(
        secretShare.getData(),
        Matchers.is(secretShareDataStore.deleteSecretShareData(testSecretId)));
    verify(minioClientMock, times(1))
        .removeObject(
            RemoveObjectArgs.builder().bucket(BUCKET).object(testSecretId.toString()).build());
  }

  @SneakyThrows
  @Test
  void
      givenDeleteCallOnMinioClientFails_whenDeleteSecretShareData_thenWrapExceptionInAmphoraServiceException() {
    SecretShare secretShare = AmphoraTestData.getRandomSecretShare(testSecretId);
    IOException expectedCause = new IOException("Totally expected that move..");

    doReturn(
            new GetObjectResponse(
                null,
                BUCKET,
                null,
                testSecretId.toString(),
                new ByteArrayInputStream(secretShare.getData())))
        .when(minioClientMock)
        .getObject(GetObjectArgs.builder().bucket(BUCKET).object(testSecretId.toString()).build());

    doThrow(expectedCause)
        .when(minioClientMock)
        .removeObject(
            RemoveObjectArgs.builder().bucket(BUCKET).object(testSecretId.toString()).build());

    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () -> secretShareDataStore.deleteSecretShareData(testSecretId));
    assertEquals(
        String.format(
            DELETE_DATA_FOR_SECRET_EXCEPTION_MSG, testSecretId, expectedCause.getMessage()),
        ase.getMessage());
    assertEquals(expectedCause, ase.getCause());
  }
}
