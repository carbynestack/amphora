/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.config;

import static io.carbynestack.amphora.service.config.BigIntegerConverter.INVALID_VALUE_EXCEPTION_MSG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import org.junit.Test;

public class BigIntegerConverterTest {

  @Test
  public void givenValidBigIntegerAsString_whenConvert_thenReturnBigIntegerObject() {
    BigInteger expected = BigInteger.valueOf(42);
    BigIntegerConverter bigIntegerConverter = new BigIntegerConverter();
    assertEquals(expected, bigIntegerConverter.convert(expected.toString()));
  }

  @Test
  public void givenNull_whenConvert_thenThrowException() {
    BigIntegerConverter bigIntegerConverter = new BigIntegerConverter();
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> bigIntegerConverter.convert(null));
    assertEquals(String.format(INVALID_VALUE_EXCEPTION_MSG, "null"), iae.getMessage());
  }

  @Test
  public void givenEmptyString_whenConvert_thenThrowException() {
    String illegalInput = "";
    BigIntegerConverter converter = new BigIntegerConverter();
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> converter.convert(illegalInput));
    assertEquals(String.format(INVALID_VALUE_EXCEPTION_MSG, illegalInput), iae.getMessage());
  }
}
