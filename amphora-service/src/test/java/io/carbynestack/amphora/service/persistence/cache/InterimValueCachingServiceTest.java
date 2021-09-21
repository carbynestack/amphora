/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.carbynestack.amphora.common.FactorPair;
import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.vavr.control.Option;
import java.math.BigInteger;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@RunWith(MockitoJUnitRunner.class)
public class InterimValueCachingServiceTest {
  @Mock RedisTemplate<String, Object> redisTemplateMock;
  @Mock AmphoraCacheProperties amphoraCachePropertiesMock;
  @Mock private ValueOperations<String, Object> valueOperationsMock;

  private final String testCacheName = "testCache";
  private final String testCachePrefix = CacheKeyPrefix.simple().compute(testCacheName);
  private final Random rnd = new Random(42);

  private InterimValueCachingService interimValueCachingService;

  @Before
  public void setup() {
    when(amphoraCachePropertiesMock.getInterimValueStore()).thenReturn(testCacheName);
    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
    interimValueCachingService =
        new InterimValueCachingService(redisTemplateMock, amphoraCachePropertiesMock);
  }

  @Test
  public void givenSuccessfulRequest_whenPuttingInterimValue_thenStoreInCacheWithCorrectKey() {
    MultiplicationExchangeObject exchangeObject = createExchangeObject();
    interimValueCachingService.putInterimValues(exchangeObject);
    verify(valueOperationsMock, times(1)).set(getCacheKey(exchangeObject), exchangeObject);
  }

  @Test
  public void
      givenValuesPresentForIdAndPlayer_whenGettingInterimValues_thenReturnOptionWithObjectAndEvict() {
    MultiplicationExchangeObject exchangeObject = createExchangeObject();
    when(redisTemplateMock.hasKey(getCacheKey(exchangeObject))).thenReturn(true);
    when(valueOperationsMock.get(getCacheKey(exchangeObject))).thenReturn(exchangeObject);
    assertEquals(
        Option.of(exchangeObject.getInterimValues()),
        interimValueCachingService.getInterimValuesAndEvict(
            exchangeObject.getOperationId(), exchangeObject.getPlayerId()));
    verify(redisTemplateMock, times(1))
        .hasKey(getCacheKey(exchangeObject.getOperationId(), exchangeObject.getPlayerId()));
    verify(redisTemplateMock, times(1))
        .delete(getCacheKey(exchangeObject.getOperationId(), exchangeObject.getPlayerId()));
  }

  @Test
  public void
      givenValuesNotPresentForIdAndPlayer_whenGettingInterimValues_thenReturnFailedOptionAndDontCallEvict() {
    UUID operationId = UUID.fromString("a7d4970c-36de-4880-b91b-6656dff3bac9");
    int playerId = 0;
    when(redisTemplateMock.hasKey(any())).thenReturn(false);
    assertThat(
        interimValueCachingService.getInterimValuesAndEvict(operationId, playerId),
        Matchers.is(Option.none()));
    verify(valueOperationsMock, never()).get(getCacheKey(operationId, playerId));
    verify(redisTemplateMock, never()).delete(getCacheKey(operationId, playerId));
  }

  private MultiplicationExchangeObject createExchangeObject() {
    return new MultiplicationExchangeObject(
        UUID.fromString("2affd2b3-bee6-4a25-9943-1fd19ff6f3b6"),
        rnd.nextInt(100),
        IntStream.range(0, rnd.nextInt(50))
            .mapToObj(i -> FactorPair.of(new BigInteger(5, rnd), new BigInteger(5, rnd)))
            .collect(Collectors.toList()));
  }

  private String getCacheKey(MultiplicationExchangeObject exchangeObject) {
    return getCacheKey(exchangeObject.getOperationId(), exchangeObject.getPlayerId());
  }

  private String getCacheKey(UUID operationid, int playerId) {
    return testCachePrefix + operationid + "_" + playerId;
  }
}
