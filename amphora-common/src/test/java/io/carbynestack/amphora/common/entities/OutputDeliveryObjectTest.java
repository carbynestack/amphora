/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import org.junit.jupiter.api.Test;

class OutputDeliveryObjectTest {
  @Test
  void givenValidBuilderConfiguration_whenCallingBuildOnBuilder_thenReturnObject() {
    byte[] secretShares = new byte[MpSpdzIntegrationUtils.WORD_WIDTH];
    byte[] rShares = new byte[MpSpdzIntegrationUtils.WORD_WIDTH];
    byte[] wShares = new byte[MpSpdzIntegrationUtils.WORD_WIDTH];
    byte[] uShares = new byte[MpSpdzIntegrationUtils.WORD_WIDTH];
    byte[] vShares = new byte[MpSpdzIntegrationUtils.WORD_WIDTH];
    OutputDeliveryObject odo =
        OutputDeliveryObject.builder()
            .secretShares(secretShares)
            .rShares(rShares)
            .wShares(wShares)
            .uShares(uShares)
            .vShares(vShares)
            .build();
    assertEquals(secretShares, odo.getSecretShares());
    assertEquals(rShares, odo.getRShares());
    assertEquals(wShares, odo.getWShares());
    assertEquals(uShares, odo.getUShares());
    assertEquals(vShares, odo.getVShares());
  }

  @Test
  void
      givenRSharesOfDifferentLengthThanSecretShares_whenCallingBuildOnBuilder_thenThrowException() {
    OutputDeliveryObject.OutputDeliveryObjectBuilder outputDeliveryObjectBuilder =
        getValidTestODO(2).toBuilder().rShares(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]);
    IllegalArgumentException actualIae =
        assertThrows(IllegalArgumentException.class, outputDeliveryObjectBuilder::build);
    assertEquals("The provided shares must be of the same length", actualIae.getMessage());
  }

  @Test
  void
      givenWSharesOfDifferentLengthThanSecretShares_whenCallingBuildOnBuilder_thenThrowException() {
    OutputDeliveryObject.OutputDeliveryObjectBuilder outputDeliveryObjectBuilder =
        getValidTestODO(2).toBuilder().wShares(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]);
    IllegalArgumentException actualIae =
        assertThrows(IllegalArgumentException.class, outputDeliveryObjectBuilder::build);
    assertEquals("The provided shares must be of the same length", actualIae.getMessage());
  }

  @Test
  void
      givenUSharesOfDifferentLengthThanSecretShares_whenCallingBuildOnBuilder_thenThrowException() {
    OutputDeliveryObject.OutputDeliveryObjectBuilder outputDeliveryObjectBuilder =
        getValidTestODO(2).toBuilder().uShares(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]);
    IllegalArgumentException actualIae =
        assertThrows(IllegalArgumentException.class, outputDeliveryObjectBuilder::build);
    assertEquals("The provided shares must be of the same length", actualIae.getMessage());
  }

  @Test
  void
      givenVSharesOfDifferentLengthThanSecretShares_whenCallingBuildOnBuilder_thenThrowException() {
    OutputDeliveryObject.OutputDeliveryObjectBuilder outputDeliveryObjectBuilder =
        getValidTestODO(2).toBuilder().vShares(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]);
    IllegalArgumentException actualIae =
        assertThrows(IllegalArgumentException.class, outputDeliveryObjectBuilder::build);
    assertEquals("The provided shares must be of the same length", actualIae.getMessage());
  }

  private OutputDeliveryObject getValidTestODO(int shareLength) {
    byte[] shares = new byte[shareLength * MpSpdzIntegrationUtils.WORD_WIDTH];
    return OutputDeliveryObject.builder()
        .secretShares(shares)
        .rShares(shares)
        .wShares(shares)
        .vShares(shares)
        .uShares(shares)
        .build();
  }
}
