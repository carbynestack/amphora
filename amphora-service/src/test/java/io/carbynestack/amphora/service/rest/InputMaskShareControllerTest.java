/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.service.rest.InputMaskShareController.REQUEST_IDENTIFIER_MUST_NOT_BE_NULL_EXCEPTION_MSG;
import static io.carbynestack.amphora.service.rest.InputMaskShareController.TOO_LESS_INPUT_MASKS_EXCEPTION_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.amphora.common.ShareFamily;
import io.carbynestack.amphora.service.AmphoraTestData;
import io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class InputMaskShareControllerTest {
  private final UUID testRequestId = UUID.fromString("7520e090-1437-44da-9e4e-eea5b2200fea");
  private final int validNumberOfTuples = 1;
  private final int invalidNumberOfTuples = -1;

  @Mock private InputMaskCachingService inputMaskStore;

  @InjectMocks private InputMaskShareController inputMaskShareController;

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenGetInputMasks_thenReturnExpectedResult() {
    TupleList<InputMask<Field.Gfp>, Field.Gfp> testInputMasks =
        AmphoraTestData.getRandomInputMaskList(validNumberOfTuples);
    OutputDeliveryObject testOdo =
        AmphoraTestData.getRandomOutputDeliveryObject(validNumberOfTuples);

    when(inputMaskStore.getInputMasksAsOutputDeliveryObject(testRequestId, testInputMasks.size(), ShareFamily.COWGEAR))
        .thenReturn(testOdo);

    ResponseEntity<OutputDeliveryObject> responseEntity =
        inputMaskShareController.getInputMasks(testRequestId, validNumberOfTuples, ShareFamily.COWGEAR.name());

    assertEquals(testOdo, responseEntity.getBody());
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
  }

  @Test
  void givenEmptyStringAsRequestId_whenGetInputMasks_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> inputMaskShareController.getInputMasks(null, validNumberOfTuples, ShareFamily.COWGEAR.getFamilyName()));
    assertEquals(REQUEST_IDENTIFIER_MUST_NOT_BE_NULL_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  void givenInvalidCountArgument_whenGetInputMasks_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> inputMaskShareController.getInputMasks(testRequestId, invalidNumberOfTuples, ShareFamily.COWGEAR.getFamilyName()));
    assertEquals(TOO_LESS_INPUT_MASKS_EXCEPTION_MSG, iae.getMessage());
  }
}
