/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.testconfig;

import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.carbynestack.amphora.service.config.MinioProperties;
import io.carbynestack.amphora.service.persistence.metadata.SecretEntityRepository;
import io.carbynestack.amphora.service.persistence.metadata.TagRepository;
import io.minio.*;
import io.minio.errors.*;
import io.vavr.control.Try;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class PersistenceTestEnvironment {
  @Autowired private TagRepository tagRepository;

  @Autowired private SecretEntityRepository secretEntityRepository;

  @Autowired private CacheManager cacheManager;

  @Autowired private AmphoraCacheProperties cacheProperties;

  @Autowired private MinioClient minioClient;

  @Autowired private MinioProperties minioProperties;

  public void clearAllData() {
    try {
      secretEntityRepository.deleteAll();
      tagRepository.deleteAll();
      Objects.requireNonNull(cacheManager.getCache(cacheProperties.getInterimValueStore())).clear();
      Objects.requireNonNull(cacheManager.getCache(cacheProperties.getInputMaskStore())).clear();
      if (minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build())) {
        Lists.newArrayList(
                minioClient
                    .listObjects(
                        ListObjectsArgs.builder().bucket(minioProperties.getBucket()).build())
                    .iterator())
            .forEach(
                itemResult ->
                    Try.run(
                        () ->
                            minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                    .bucket(minioProperties.getBucket())
                                    .object(itemResult.get().objectName())
                                    .build())));
        minioClient.removeBucket(
            RemoveBucketArgs.builder().bucket(minioProperties.getBucket()).build());
      }
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
    } catch (ErrorResponseException
        | InsufficientDataException
        | InternalException
        | InvalidKeyException
        | InvalidResponseException
        | NoSuchAlgorithmException
        | ServerException
        | XmlParserException
        | IOException e) {
      throw new IllegalStateException("Failed clearing persisted data.", e);
    }
  }
}
