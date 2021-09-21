/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.cache;

import io.carbynestack.amphora.common.FactorPair;
import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service used to cache {@link MultiplicationExchangeObject}s, shared by players of the MPC
 * cluster to open values when performing a MPC based multiplication.
 */
@Service
public class InterimValueCachingService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final String cachePrefix;

  @Autowired
  public InterimValueCachingService(
      RedisTemplate<String, Object> redisTemplate, AmphoraCacheProperties cacheProperties) {
    this.redisTemplate = redisTemplate;
    this.cachePrefix = CacheKeyPrefix.simple().compute(cacheProperties.getInterimValueStore());
  }

  /**
   * Stores a {@link MultiplicationExchangeObject} in the cache.<br>
   * The {@link MultiplicationExchangeObject}'s {@link MultiplicationExchangeObject#getOperationId()
   * operationId} combined with the {@link MultiplicationExchangeObject#getPlayerId() playerId} is
   * used as a key to reference the stored secret.
   *
   * @param exchangeObject the {@link MultiplicationExchangeObject} to cache
   */
  @Transactional
  public void putInterimValues(MultiplicationExchangeObject exchangeObject) {
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set(
        getCacheKey(exchangeObject.getOperationId(), exchangeObject.getPlayerId()), exchangeObject);
  }

  /**
   * Returns a {@link MultiplicationExchangeObject}, which is referenced by a combination of the
   * given operationId and playerId, taken from the cache.<br>
   * This operation removes the stored data from the cache.
   *
   * @param operationId operationId used as part of the key to reference the {@link
   *     MultiplicationExchangeObject} in the cache
   * @param playerId playerId used as part of the key to reference the {@link
   *     MultiplicationExchangeObject} in the cache
   * @return an {@link Option} either containing interim values stored by the referenced {@link
   *     MultiplicationExchangeObject} or {@link Option.None} if no data was stored with the given
   *     key
   */
  @Transactional
  public Option<List<FactorPair>> getInterimValuesAndEvict(UUID operationId, int playerId) {
    Option<List<FactorPair>> interimValues = Option.none();
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    String key = getCacheKey(operationId, playerId);
    if (redisTemplate.hasKey(key)) {
      interimValues =
          Try.of(() -> ((MultiplicationExchangeObject) ops.get(key)).getInterimValues()).toOption();
      redisTemplate.delete(key);
    }
    return interimValues;
  }

  private String getCacheKey(UUID operationId, int playerId) {
    return cachePrefix + operationId + "_" + playerId;
  }
}
