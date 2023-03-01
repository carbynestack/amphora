/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.carbynestack.amphora.common.MaskedInputData;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import org.junit.jupiter.api.Test;

public class MaskedInputDataTest {

  @Test
  void givenInvalidValue_whenCreatingMaskedInputData_thenThrowException() {
    IllegalArgumentException actualIae =
        assertThrows(
            IllegalArgumentException.class,
            () -> MaskedInputData.of(new byte[MpSpdzIntegrationUtils.WORD_WIDTH - 1]));
    assertEquals(
        String.format(
            "Length of a Masked Input value has to be %s bytes.",
            MpSpdzIntegrationUtils.WORD_WIDTH),
        actualIae.getMessage());
  }
}
