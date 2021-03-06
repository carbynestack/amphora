/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.cache;

import static io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService.NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG;
import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.AmphoraTestData;
import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.common.CastorServiceUri;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@RunWith(MockitoJUnitRunner.class)
public class InputMaskStoreTest {
  private final CastorServiceUri castorServiceUri =
      new CastorServiceUri("https://castor.carbynestack.io");
  private final List<CastorServiceUri> castorServiceUris = singletonList(castorServiceUri);
  private final UUID testRequestId = UUID.fromString("93448822-fc76-4989-a927-450486ae0a08");

  @Mock private CastorIntraVcpClient interVcpClientMock;
  @Mock RedisTemplate<String, Object> redisTemplateMock;
  @Mock AmphoraCacheProperties amphoraCachePropertiesMock;
  @Mock private ValueOperations<String, Object> valueOperationsMock;

  private final String testCacheName = "testCache";
  private final String testCachePrefix = CacheKeyPrefix.simple().compute(testCacheName);

  private InputMaskCachingService inputMaskCache;

  @Before
  public void setup() {
    when(amphoraCachePropertiesMock.getInputMaskStore()).thenReturn(testCacheName);
    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
    inputMaskCache =
        new InputMaskCachingService(
            interVcpClientMock, redisTemplateMock, amphoraCachePropertiesMock);
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenPutInputMasks_thenStoreInCacheAndReturnList() {
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasks =
        AmphoraTestData.getRandomInputMaskList(5);

    when(interVcpClientMock.downloadTupleShares(testRequestId, INPUT_MASK_GFP, inputMasks.size()))
        .thenReturn(inputMasks);
    assertEquals(
        inputMasks, inputMaskCache.fetchAndCacheInputMasks(testRequestId, inputMasks.size()));
    verify(valueOperationsMock, times(1)).set(testCachePrefix + testRequestId, inputMasks);
  }

  @Test
  public void givenInputMasksInCache_whenGetInputMasks_thenReturnList() {
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasks =
        AmphoraTestData.getRandomInputMaskList(5);
    when(valueOperationsMock.get(testCachePrefix + testRequestId)).thenReturn(inputMasks);
    assertEquals(inputMasks, inputMaskCache.getInputMasks(testRequestId));
  }

  @Test
  public void givenInputMasksNotInCache_whenGetInputMasks_thenThrowException() {
    when(valueOperationsMock.get(testCachePrefix + testRequestId)).thenReturn(null);
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class, () -> inputMaskCache.getInputMasks(testRequestId));
    assertEquals(
        String.format(NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG, testRequestId),
        ase.getMessage());
  }

  @Test
  public void givenSuccessfulRequest_whenRemoveInputMasks_thenRemoveFromCache() {
    inputMaskCache.removeInputMasks(testRequestId);
    verify(redisTemplateMock, times(1)).delete(testCachePrefix + testRequestId);
  }
}
