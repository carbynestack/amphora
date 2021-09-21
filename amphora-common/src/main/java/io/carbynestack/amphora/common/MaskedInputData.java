/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * A convenience class used to store the data of a {@link MaskedInput} and ensure the strict length
 * requirements for the stored data. <br>
 * The data supposed to be in the gfp byte representation as used by the SPDZ based MPC
 * implementations. This data is stored in a byte array must be of the exact word length as defined
 * by the {@link MpSpdzIntegrationUtils} (see {@link MpSpdzIntegrationUtils#WORD_WIDTH}).
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskedInputData implements Serializable {
  private static final long serialVersionUID = 7413845284141865341L;
  /**
   * The actual masked data. <br>
   * The data is expected to be in the gfp byte representation as used by SPDZ and must be of the
   * exact word as defined by the {@link MpSpdzIntegrationUtils} (see {@link
   * MpSpdzIntegrationUtils#WORD_WIDTH}).
   */
  byte[] value;

  /**
   * Returns a new {@link MaskedInputData} with the given data. <br>
   * This method will ensure the strict length requirements for the data to be fulfilled (see {@link
   * #value})
   *
   * @param value the actual data
   * @return a new {@link MaskedInputData} object
   * @throws IllegalArgumentException if the length of the given data does not match the required
   *     word length
   */
  public static MaskedInputData of(byte[] value) {
    if (value == null || value.length != MpSpdzIntegrationUtils.WORD_WIDTH) {
      throw new IllegalArgumentException(
          String.format(
              "Length of a Masked Input value has to be %s bytes.",
              MpSpdzIntegrationUtils.WORD_WIDTH));
    }
    return new MaskedInputData(value);
  }
}
