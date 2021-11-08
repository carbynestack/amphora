/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

import io.carbynestack.amphora.common.MaskedInput;
import io.carbynestack.amphora.common.MaskedInputData;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MaskedInputTest {

  @Test
  void givenIdIsNull_whenCreatingNewSecret_thenThrowException() {
    UUID nullId = null;
    List<MaskedInputData> maskedInputData = emptyList();
    List<Tag> tags = emptyList();
    NullPointerException actualNpe =
        assertThrows(
            NullPointerException.class, () -> new MaskedInput(nullId, maskedInputData, tags));
    assertEquals("secretId is marked non-null but is null", actualNpe.getMessage());
  }

  @Test
  void givenTagsAreNull_whenCreatingMaskedInput_thenUseEmptyListInstead() {
    UUID secretId = UUID.fromString("80fbba1b-3da8-4b1e-8a2c-cebd65229fad");
    List<MaskedInputData> maskedInputData =
        Collections.singletonList(MaskedInputData.of(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]));
    MaskedInput maskedInput = new MaskedInput(secretId, maskedInputData, emptyList());
    assertEquals(secretId, maskedInput.getSecretId());
    assertEquals(maskedInputData, maskedInput.getData());
    assertNotNull(maskedInput.getTags(), "Tags must be replaced by empty list");
    assertTrue(maskedInput.getTags().isEmpty(), "Tags must be empty");
  }
}
