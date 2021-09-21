/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.cache;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;

import io.carbynestack.amphora.client.Secret;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import io.carbynestack.castor.common.exceptions.CastorClientException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@link InputMaskCachingService} is used to store {@link InputMask}s that are required when
 * uploading new {@link Secret}s.<br>
 * The {@link InputMask}s are stored as a {@link TupleList}, referenced by a unique identifier.
 *
 * <p>This service is used to cache {@link InputMask}s that were requested by a client to share and
 * upload a new {@link Secret}. While the {@link InputMask} is required on client side to mask a
 * secret (see {@link io.carbynestack.amphora.common.MaskedInput}), each <i>Amphora</i> service
 * needs its individual share of the used {@link InputMask} to generate its unique share of the
 * secret.
 */
@Slf4j
@Service
public class InputMaskCachingService {
  public static final String NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG =
      "No InputMaskList available in Cache for requestId %s";
  private final RedisTemplate<String, Object> redisTemplate;
  private final CastorIntraVcpClient castorClient;
  private final String cachePrefix;

  @Autowired
  public InputMaskCachingService(
      CastorIntraVcpClient castorClient,
      RedisTemplate<String, Object> redisTemplate,
      AmphoraCacheProperties cacheProperties) {
    this.castorClient = castorClient;
    this.redisTemplate = redisTemplate;
    this.cachePrefix = CacheKeyPrefix.simple().compute(cacheProperties.getInputMaskStore());
  }

  /**
   * Fetches {@link InputMask}s from Castor, stores them as a {@link TupleList} in the cache,
   * referenced by a unique identifier, and returns the {@link TupleList}.
   *
   * @param requestId a unique identifier used as a key to access the stored {@link TupleList} in
   *     the cache
   * @param count number of requested {@link InputMask}s.
   * @return the given list of {@link InputMask}s
   * @throws CastorClientException if composing the request tuples URI failed
   * @throws CastorClientException if downloading the tuples from the service failed
   */
  @Transactional
  public TupleList<InputMask<Field.Gfp>, Field.Gfp> fetchAndCacheInputMasks(
      UUID requestId, long count) {
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMaskShares =
        castorClient.downloadTupleShares(requestId, INPUT_MASK_GFP, count);
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set(cachePrefix + requestId, inputMaskShares);
    return inputMaskShares;
  }

  /**
   * Return the cached {@link InputMask}s linked to a given identifier
   *
   * @param requestId unique identifier linked with the requested {@link InputMask}s
   * @return List of {@link InputMask}s
   * @throws AmphoraServiceException if no {@link InputMask}s were reserved fo the given requestId
   * @throws IllegalStateException if
   */
  @Transactional(readOnly = true)
  public TupleList<InputMask<Field.Gfp>, Field.Gfp> getInputMasks(UUID requestId) {
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasks =
        (TupleList<InputMask<Field.Gfp>, Field.Gfp>) ops.get(cachePrefix + requestId);
    if (inputMasks == null) {
      throw new AmphoraServiceException(
          String.format(NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG, requestId));
    }
    return inputMasks;
  }

  /**
   * Removes a cached {@link TupleList} from the cache, referenced by a given identifier.
   *
   * @param requestId a unique identifier used as a key for the cached {@link TupleList}
   */
  @Transactional
  public void removeInputMasks(UUID requestId) {
    redisTemplate.delete(cachePrefix + requestId);
  }
}
