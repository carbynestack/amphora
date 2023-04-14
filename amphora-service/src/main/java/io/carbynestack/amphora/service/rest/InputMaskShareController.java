/*
 * Copyright (c) 2021-2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.DOWNLOAD_INPUT_MASKS_ENDPOINT;
import static org.springframework.util.Assert.*;

import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping(path = DOWNLOAD_INPUT_MASKS_ENDPOINT)
public class InputMaskShareController {
  public static final String REQUEST_IDENTIFIER_MUST_NOT_BE_NULL_EXCEPTION_MSG =
      "Request identifier must not be null.";
  public static final String TOO_LESS_INPUT_MASKS_EXCEPTION_MSG =
      "The number of requested Input Masks has to be 1 or greater.";
  private final InputMaskCachingService inputMaskCachingService;

  @GetMapping
  public ResponseEntity<OutputDeliveryObject> getInputMasks(
      @RequestParam UUID requestId, @RequestParam long count) {
    notNull(requestId, REQUEST_IDENTIFIER_MUST_NOT_BE_NULL_EXCEPTION_MSG);
    isTrue(count > 0, TOO_LESS_INPUT_MASKS_EXCEPTION_MSG);
    return new ResponseEntity<>(
        inputMaskCachingService.getInputMasksAsOutputDeliveryObject(requestId, count),
        HttpStatus.OK);
  }
}
