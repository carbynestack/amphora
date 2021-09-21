/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.config;

import java.math.BigInteger;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class BigIntegerConverter implements Converter<String, BigInteger> {
  public static final String INVALID_VALUE_EXCEPTION_MSG =
      "Invalid value \"%s\" for type BigInteger";

  @Override
  public BigInteger convert(String in) {
    if (in == null || in.isEmpty()) {
      throw new IllegalArgumentException(String.format(INVALID_VALUE_EXCEPTION_MSG, in));
    }
    return new BigInteger(in);
  }
}
