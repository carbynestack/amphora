/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.cache;

import static io.carbynestack.amphora.service.AmphoraTestData.extractTupleValuesFromInputMaskList;
import static io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService.NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG;
import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.amphora.common.ShareFamily;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.AmphoraTestData;
import io.carbynestack.amphora.service.calculation.OutputDeliveryService;
import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.common.CastorServiceUri;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleFamily;
import io.carbynestack.castor.common.entities.TupleList;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class InputMaskStoreTest {
  private final CastorServiceUri castorServiceUri =
      new CastorServiceUri("https://castor.carbynestack.io");
  private final List<CastorServiceUri> castorServiceUris = singletonList(castorServiceUri);
  private final UUID testRequestId = UUID.fromString("93448822-fc76-4989-a927-450486ae0a08");
  private final UUID testOdoRequestId =
      UUID.nameUUIDFromBytes(String.format("%s_odo-computation", testRequestId).getBytes());

  @Mock private CastorIntraVcpClient interVcpClientMock;
  @Mock RedisTemplate<String, Object> redisTemplateMock;
  @Mock AmphoraCacheProperties amphoraCachePropertiesMock;
  @Mock private ValueOperations<String, Object> valueOperationsMock;
  @Mock private OutputDeliveryService outputDeliveryServiceMock;

  private final String testCacheName = "testCache";
  private final String testCachePrefix = CacheKeyPrefix.simple().compute(testCacheName);

  private InputMaskCachingService inputMaskCache;

  @BeforeEach
  public void setup() {
    when(amphoraCachePropertiesMock.getInputMaskStore()).thenReturn(testCacheName);
    inputMaskCache =
        new InputMaskCachingService(
            interVcpClientMock,
            redisTemplateMock,
            amphoraCachePropertiesMock,
            outputDeliveryServiceMock);
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenPutInputMasks_thenStoreInCacheAndReturnList() {
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasks =
        AmphoraTestData.getRandomInputMaskList(5);
    OutputDeliveryObject odo = AmphoraTestData.getRandomOutputDeliveryObject(5);

    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
    when(interVcpClientMock.downloadTupleShares(testRequestId, INPUT_MASK_GFP, inputMasks.size(), TupleFamily.COWGEAR))
        .thenReturn(inputMasks);
    when(outputDeliveryServiceMock.computeOutputDeliveryObject(
            extractTupleValuesFromInputMaskList(inputMasks), testOdoRequestId, ShareFamily.COWGEAR))
        .thenReturn(odo);
    assertEquals(
        odo, inputMaskCache.getInputMasksAsOutputDeliveryObject(testRequestId, inputMasks.size(), ShareFamily.COWGEAR));
    verify(valueOperationsMock, times(1)).set(testCachePrefix + testRequestId, inputMasks);
  }

  @Test
  void givenInputMasksInCache_whenGetInputMasks_thenReturnList() {
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasks =
        AmphoraTestData.getRandomInputMaskList(5);
    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
    when(valueOperationsMock.get(testCachePrefix + testRequestId)).thenReturn(inputMasks);
    assertEquals(inputMasks, inputMaskCache.getCachedInputMasks(testRequestId));
  }

  @Test
  void givenInputMasksNotInCache_whenGetInputMasks_thenThrowException() {
    when(redisTemplateMock.opsForValue()).thenReturn(valueOperationsMock);
    when(valueOperationsMock.get(testCachePrefix + testRequestId)).thenReturn(null);
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class, () -> inputMaskCache.getCachedInputMasks(testRequestId));
    assertEquals(
        String.format(NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG, testRequestId),
        ase.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenRemoveInputMasks_thenRemoveFromCache() {
    inputMaskCache.removeInputMasks(testRequestId);
    verify(redisTemplateMock, times(1)).delete(testCachePrefix + testRequestId);
  }
}
