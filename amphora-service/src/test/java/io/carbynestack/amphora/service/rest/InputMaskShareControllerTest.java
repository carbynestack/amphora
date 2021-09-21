/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.service.rest.InputMaskShareController.REQUEST_IDENTIFIER_MUST_NOT_BE_NULL_EXCEPTION_MSG;
import static io.carbynestack.amphora.service.rest.InputMaskShareController.TOO_LESS_INPUT_MASKS_EXCEPTION_MSG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import io.carbynestack.amphora.service.AmphoraTestData;
import io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService;
import io.carbynestack.castor.common.CastorServiceUri;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class InputMaskShareControllerTest {
  private final UUID testRequestId = UUID.fromString("7520e090-1437-44da-9e4e-eea5b2200fea");
  private final long validNumberOfTuples = 1;
  private final long invalidNumberOfTuples = -1;
  private final String testCastorServiceUri = "https://castor.carbynestack.io";
  private final TupleList<InputMask<Field.Gfp>, Field.Gfp> testInputMasks =
      AmphoraTestData.getRandomInputMaskList(validNumberOfTuples);

  @Mock private InputMaskCachingService inputMaskStore;

  @InjectMocks private InputMaskShareController inputMaskShareController;

  @Before
  public void prepareMocks() {
    Map<CastorServiceUri, TupleList> inputMaskListMap = new HashMap<>();
    CastorServiceUri castorServiceUri = new CastorServiceUri(this.testCastorServiceUri);
    inputMaskListMap.put(castorServiceUri, testInputMasks);

    when(inputMaskStore.fetchAndCacheInputMasks(testRequestId, testInputMasks.size()))
        .thenReturn(testInputMasks);
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenGetInputMasks_thenReturnExpectedResult() {
    ResponseEntity<TupleList<InputMask<Field.Gfp>, Field.Gfp>> responseEntity =
        inputMaskShareController.getInputMasks(testRequestId, validNumberOfTuples);

    assertThat(responseEntity.getBody(), is(equalTo(testInputMasks)));
    assertThat(responseEntity.getStatusCode(), is(equalTo(HttpStatus.OK)));
  }

  @Test
  public void givenEmptyStringAsRequestId_whenGetInputMasks_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> inputMaskShareController.getInputMasks(null, validNumberOfTuples));
    assertEquals(REQUEST_IDENTIFIER_MUST_NOT_BE_NULL_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  public void givenInvalidCountArgument_whenGetInputMasks_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> inputMaskShareController.getInputMasks(testRequestId, invalidNumberOfTuples));
    assertEquals(TOO_LESS_INPUT_MASKS_EXCEPTION_MSG, iae.getMessage());
  }
}
