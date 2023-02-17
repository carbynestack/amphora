/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.datastore;

import static org.apache.commons.io.IOUtils.toByteArray;

import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.config.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An implementation of the {@link SecretShareDataStore} interface backed by a Minio database to
 * persist secrets data.
 *
 * <p>The implementation will utilize the {@link MinioClient}'s functionality to efficiently upload,
 * store amd retrieve data. The data will therefore be processed as {@link ByteArrayInputStream}s
 * which allow for streaming large data chunks to the database.
 */
@Service
public class MinioSecretShareDataStore implements SecretShareDataStore {
  public static final String GET_DATA_FOR_SECRET_EXCEPTION_MSG =
      "Data could not be retrieved for secret share #%s: %s";
  public static final String DELETE_DATA_FOR_SECRET_EXCEPTION_MSG =
      "Error while deleting data for secret share #%s: %s";
  public static final String FAILED_INITIALIZING_EXCEPTION_MSG =
      "Failed initializing MinoSecretShareDataStore.";
  private final MinioClient minioClient;
  private final MinioProperties minioProperties;

  @Autowired
  public MinioSecretShareDataStore(MinioClient minioClient, MinioProperties minioProperties) {
    this.minioClient = minioClient;
    this.minioProperties = minioProperties;

    try {
      if (!minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build())) {
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
      }
    } catch (Exception e) {
      throw new AmphoraServiceException(FAILED_INITIALIZING_EXCEPTION_MSG, e);
    }
  }

  @Override
  public void storeSecretShareData(UUID id, byte[] data) {
    try (InputStream inputStream = new ByteArrayInputStream(data)) {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(minioProperties.getBucket()).object(id.toString()).stream(
                  inputStream, data.length, -1)
              .build());
    } catch (InvalidKeyException
        | IOException
        | ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidResponseException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException e) {
      throw new AmphoraServiceException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] getSecretShareData(UUID id) {
    try (InputStream dataStream =
        minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(id.toString())
                .build())) {
      return toByteArray(dataStream);
    } catch (Exception e) {
      throw new AmphoraServiceException(
          String.format(GET_DATA_FOR_SECRET_EXCEPTION_MSG, id.toString(), e.getMessage()), e);
    }
  }

  @Override
  public byte[] deleteSecretShareData(UUID id) {
    byte[] shareToDelete = getSecretShareData(id);
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(minioProperties.getBucket())
              .object(id.toString())
              .build());
    } catch (Exception e) {
      throw new AmphoraServiceException(
          String.format(DELETE_DATA_FOR_SECRET_EXCEPTION_MSG, id, e.getMessage()), e);
    }
    return shareToDelete;
  }
}
