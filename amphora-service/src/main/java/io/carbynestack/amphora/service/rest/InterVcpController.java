/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.INTER_VCP_OPERATIONS_SEGMENT;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.OPEN_INTERIM_VALUES_ENDPOINT;

import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.service.persistence.cache.InterimValueCachingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = INTER_VCP_OPERATIONS_SEGMENT)
@RequiredArgsConstructor
public class InterVcpController {
  private final InterimValueCachingService interimValueCachingService;

  @PostMapping(path = OPEN_INTERIM_VALUES_ENDPOINT)
  public ResponseEntity<Void> open(@RequestBody MultiplicationExchangeObject exchangeObject) {
    log.debug("received interim values for operation #{}", exchangeObject.getOperationId());
    interimValueCachingService.putInterimValues(exchangeObject);
    return ResponseEntity.accepted().build();
  }
}
