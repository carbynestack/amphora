/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.cache;

import static io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService.NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG;
import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.AmphoraServiceApplication;
import io.carbynestack.amphora.service.AmphoraTestData;
import io.carbynestack.amphora.service.config.AmphoraCacheProperties;
import io.carbynestack.amphora.service.config.CastorClientProperties;
import io.carbynestack.amphora.service.testconfig.PersistenceTestEnvironment;
import io.carbynestack.amphora.service.testconfig.ReusableMinioContainer;
import io.carbynestack.amphora.service.testconfig.ReusablePostgreSQLContainer;
import io.carbynestack.amphora.service.testconfig.ReusableRedisContainer;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import java.util.UUID;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    classes = {AmphoraServiceApplication.class})
@ActiveProfiles(profiles = {"test"})
public class InputMaskStoreRedisIT {
  private final UUID testRequestId = UUID.fromString("b380f595-f301-4577-9f44-6d78728c38c3");
  private final long numberOfTuples = 42;
  private final TupleList testInputMasks1 = AmphoraTestData.getRandomInputMaskList(numberOfTuples);
  private final TupleList testInputMasks2 = AmphoraTestData.getRandomInputMaskList(numberOfTuples);

  @ClassRule
  public static ReusableRedisContainer reusableRedisContainer =
      ReusableRedisContainer.getInstance();

  @ClassRule
  public static ReusableMinioContainer reusableMinioContainer =
      ReusableMinioContainer.getInstance();

  @ClassRule
  public static ReusablePostgreSQLContainer reusablePostgreSQLContainer =
      ReusablePostgreSQLContainer.getInstance();

  @Autowired private PersistenceTestEnvironment persistenceTestEnvironment;

  @Autowired private CastorClientProperties castorClientProperties;

  @Autowired private InputMaskCachingService inputMaskStore;

  @Autowired private AmphoraCacheProperties cacheProperties;

  @Autowired private CacheManager cacheManager;

  private Cache inputMaskCache;

  @MockBean private CastorIntraVcpClient castorClient;

  @Before
  public void setUp() {
    if (inputMaskCache == null) {
      inputMaskCache = cacheManager.getCache(cacheProperties.getInputMaskStore());
    }
    persistenceTestEnvironment.clearAllData();
  }

  @Test
  public void givenNoDataForKeyInCache_whenGetInputMasks_thenThrowAmphoraServiceException() {
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class, () -> inputMaskStore.getInputMasks(testRequestId));
    assertEquals(
        String.format(NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG, testRequestId),
        ase.getMessage());
  }

  @SneakyThrows
  @Test
  public void givenMultipleRequestsForSameKey_whenGetInputMasks_thenReturnSameResult() {
    when(castorClient.downloadTupleShares(testRequestId, INPUT_MASK_GFP, testInputMasks1.size()))
        .thenReturn(testInputMasks1);
    inputMaskStore.fetchAndCacheInputMasks(testRequestId, testInputMasks1.size());
    assertThat(
        inputMaskStore.getInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks1.toArray(new InputMask[0])));
    assertThat(
        inputMaskStore.getInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks1.toArray(new InputMask[0])));
  }

  @SneakyThrows
  @Test
  public void
      givenConsecutiveCallsForSameKey_whenFetchAndCacheInputMasks_thenReplaceExistingData() {
    when(castorClient.downloadTupleShares(testRequestId, INPUT_MASK_GFP, testInputMasks1.size()))
        .thenReturn(testInputMasks1)
        .thenReturn(testInputMasks2);
    assertThat(
        inputMaskStore.fetchAndCacheInputMasks(testRequestId, testInputMasks1.size()),
        Matchers.containsInRelativeOrder(testInputMasks1.toArray(new InputMask[0])));
    assertThat(
        inputMaskStore.fetchAndCacheInputMasks(testRequestId, testInputMasks2.size()),
        Matchers.containsInRelativeOrder(testInputMasks2.toArray(new InputMask[0])));
    assertThat(
        inputMaskStore.getInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks2.toArray(new InputMask[0])));
    assertNotEquals(
        testInputMasks1.toArray(new InputMask[0]),
        inputMaskStore.getInputMasks(testRequestId).toArray(new InputMask[0]));
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenRemoveInputMasks_thenDeleteFromCache() {
    inputMaskCache.put(testRequestId, testInputMasks1);
    assertThat(
        inputMaskStore.getInputMasks(testRequestId),
        Matchers.containsInRelativeOrder(testInputMasks1.toArray(new InputMask[0])));
    inputMaskStore.removeInputMasks(testRequestId);
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class, () -> inputMaskStore.getInputMasks(testRequestId));
    assertEquals(
        String.format(NO_INPUT_MASKS_FOUND_FOR_REQUEST_ID_EXCEPTION_MSG, testRequestId),
        ase.getMessage());
  }
}
