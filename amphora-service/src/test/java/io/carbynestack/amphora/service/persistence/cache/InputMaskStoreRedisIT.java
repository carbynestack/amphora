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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.amphora.common.ShareFamily;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.AmphoraServiceApplication;
import io.carbynestack.amphora.service.AmphoraTestData;
import io.carbynestack.amphora.service.calculation.OutputDeliveryService;
import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.carbynestack.amphora.service.config.CastorClientProperties;
import io.carbynestack.amphora.service.testconfig.PersistenceTestEnvironment;
import io.carbynestack.amphora.service.testconfig.ReusableMinioContainer;
import io.carbynestack.amphora.service.testconfig.ReusablePostgreSQLContainer;
import io.carbynestack.amphora.service.testconfig.ReusableRedisContainer;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleFamily;
import io.carbynestack.castor.common.entities.TupleList;
import java.util.UUID;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    classes = {AmphoraServiceApplication.class})
@ActiveProfiles(profiles = {"test"})
@Testcontainers
public class InputMaskStoreRedisIT {
  private final UUID testRequestId = UUID.fromString("b380f595-f301-4577-9f44-6d78728c38c3");
  private final int numberOfTuples = 42;
  private final TupleList<InputMask<Field.Gfp>, Field.Gfp> testInputMasks1 =
      AmphoraTestData.getRandomInputMaskList(numberOfTuples);
  private final TupleList<InputMask<Field.Gfp>, Field.Gfp> testInputMasks2 =
      AmphoraTestData.getRandomInputMaskList(numberOfTuples);
  private final OutputDeliveryObject testOutputDeliveryObject1 =
      AmphoraTestData.getRandomOutputDeliveryObject(numberOfTuples);
  private final OutputDeliveryObject testOutputDeliveryObject2 =
      AmphoraTestData.getRandomOutputDeliveryObject(numberOfTuples);

  @Container
  public static ReusableRedisContainer reusableRedisContainer =
      ReusableRedisContainer.getInstance();

  @Container
  public static ReusableMinioContainer reusableMinioContainer =
      ReusableMinioContainer.getInstance();

  @Container
  public static ReusablePostgreSQLContainer reusablePostgreSQLContainer =
      ReusablePostgreSQLContainer.getInstance();

  @Autowired private PersistenceTestEnvironment persistenceTestEnvironment;

  @Autowired private CastorClientProperties castorClientProperties;

  @Autowired private InputMaskCachingService inputMaskStore;

  @Autowired private AmphoraCacheProperties cacheProperties;

  @Autowired private CacheManager cacheManager;

  private Cache inputMaskCache;

  @MockBean private CastorIntraVcpClient castorClient;
  @MockBean private OutputDeliveryService outputDeliveryService;

  @BeforeEach
  public void setUp() {
    if (inputMaskCache == null) {
      inputMaskCache = cacheManager.getCache(cacheProperties.getInputMaskStore());
    }
    persistenceTestEnvironment.clearAllData();
  }

  @Test
  void givenNoDataForKeyInCache_whenGetInputMasks_thenThrowAmphoraServiceException() {
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class, () -> inputMaskStore.getCachedInputMasks(testRequestId));
    assertEquals(
        String.format(NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG, testRequestId),
        ase.getMessage());
  }

  @SneakyThrows
  @Test
  void givenMultipleRequestsForSameKey_whenGetInputMasks_thenReturnSameResult() {
    when(castorClient.downloadTupleShares(testRequestId, INPUT_MASK_GFP, testInputMasks1.size(), TupleFamily.COWGEAR))
        .thenReturn(testInputMasks1);
    inputMaskStore.getInputMasksAsOutputDeliveryObject(testRequestId, testInputMasks1.size(), ShareFamily.COWGEAR);
    assertThat(
        inputMaskStore.getCachedInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks1.toArray(new InputMask[0])));
    assertThat(
        inputMaskStore.getCachedInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks1.toArray(new InputMask[0])));
  }

  @SneakyThrows
  @Test
  void givenConsecutiveCallsForSameKey_whenFetchAndCacheInputMasks_thenReplaceExistingData() {
    UUID testOdoRequestId =
        UUID.nameUUIDFromBytes(String.format("%s_odo-computation", testRequestId).getBytes());
    when(castorClient.downloadTupleShares(testRequestId, INPUT_MASK_GFP, testInputMasks1.size(), TupleFamily.COWGEAR))
        .thenReturn(testInputMasks1)
        .thenReturn(testInputMasks2);
    byte[] testInputMasks1Values = extractTupleValuesFromInputMaskList(testInputMasks1);
    byte[] testInputMasks2Values = extractTupleValuesFromInputMaskList(testInputMasks2);
    when(outputDeliveryService.computeOutputDeliveryObject(testInputMasks1Values, testOdoRequestId, ShareFamily.COWGEAR))
        .thenReturn(testOutputDeliveryObject1);
    when(outputDeliveryService.computeOutputDeliveryObject(testInputMasks2Values, testOdoRequestId, ShareFamily.COWGEAR))
        .thenReturn(testOutputDeliveryObject2);
    assertEquals(
        testOutputDeliveryObject1,
        inputMaskStore.getInputMasksAsOutputDeliveryObject(testRequestId, testInputMasks1.size(), ShareFamily.COWGEAR));
    assertEquals(
        testOutputDeliveryObject2,
        inputMaskStore.getInputMasksAsOutputDeliveryObject(testRequestId, testInputMasks2.size(), ShareFamily.COWGEAR));
    assertThat(
        inputMaskStore.getCachedInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks2.toArray(new InputMask[0])));
    assertNotEquals(
        testInputMasks1.toArray(new InputMask[0]),
        inputMaskStore.getCachedInputMasks(testRequestId).toArray(new InputMask[0]));
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenRemoveInputMasks_thenDeleteFromCache() {
    inputMaskCache.put(testRequestId, testInputMasks1);
    assertThat(
        inputMaskStore.getCachedInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks1.toArray(new InputMask[0])));
    inputMaskStore.removeInputMasks(testRequestId);
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class, () -> inputMaskStore.getCachedInputMasks(testRequestId));
    assertEquals(
        String.format(NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG, testRequestId),
        ase.getMessage());
  }
}
