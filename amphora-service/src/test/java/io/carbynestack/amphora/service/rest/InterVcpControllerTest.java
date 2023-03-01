/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.service.persistence.cache.InterimValueCachingService;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InterVcpControllerTest {
  @Mock private InterimValueCachingService interimValueCachingService;

  @InjectMocks private InterVcpController interVcpController;

  @Test
  void givenSuccessfulRequest_whenReceivingInterimValues_thenStoreValuesInCache() {
    MultiplicationExchangeObject exchangeObject =
        new MultiplicationExchangeObject(
            UUID.fromString("ea983b9b-0e98-4cbb-8dbf-3c8362653c0d"), 0, Collections.emptyList());
    assertThat(interVcpController.open(exchangeObject).getStatusCode(), is(HttpStatus.ACCEPTED));
    verify(interimValueCachingService, times(1)).putInterimValues(exchangeObject);
  }
}
