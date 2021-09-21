/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common;

import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Enumeration for the allowed value type of a {@link Tag} (see {@link Tag#value}).
 *
 * <p>Possible values are
 *
 * <ul>
 *   <li>{@link TagValueType#STRING}
 *   <li>{@link TagValueType#LONG}
 * </ul>
 *
 * <p>Each type comes with a function used to validate a given string to match the type's individual
 * requirements.<br>
 * In general, a value must neither be <i>null</i> nor exceed a maximum length of {@value
 * MAX_VALUE_LENGTH}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TagValueType implements Consumer<String> {
  /**
   * Excepts the given value to meet the following requirements:
   *
   * <ul>
   *   <li>must not be <i>null</i>
   *   <li>must have a maximum length of {@value MAX_VALUE_LENGTH}
   * </ul>
   */
  STRING() {
    /**
     * Verifies the given value to be neither <i>null</i> nor exceed a maximum length of {@value
     * MAX_VALUE_LENGTH}
     *
     * @param value value to validate
     * @throws IllegalArgumentException if conditions are violated
     */
    @Override
    public void accept(String value) {
      TagValueType.validate(value);
    }
  },
  /**
   * Excepts the given value to meet the following requirements:
   *
   * <ul>
   *   <li>must not be <i>null</i>
   *   <li>must have a maximum length of {@value MAX_VALUE_LENGTH}
   *   <li>must allow to be parsed as {@link Long} (see {@link Long#parseLong(String)})
   * </ul>
   */
  LONG() {
    /**
     * Verifies the given value to be neither <i>null</i> nor exceed a maximum length of {@value
     * MAX_VALUE_LENGTH} and can be parsed as a <i>long</i>.
     *
     * @param value value to validate
     * @throws IllegalArgumentException if requirements are violated
     */
    @Override
    public void accept(String value) {
      TagValueType.validate(value);
      try {
        Long.parseLong(value);
      } catch (Exception e) {
        throw new IllegalArgumentException("Value cannot be parsed as long.", e);
      }
    }
  };

  /** Maximum allowed length for a Tag's {@link Tag#value value} */
  static final int MAX_VALUE_LENGTH = 256;

  /**
   * Verifies that a given value meets the general requirements for a tag's value (see {@link
   * TagValueType})
   *
   * @param value value to validate
   * @throws IllegalArgumentException if requirements are violated
   */
  private static final void validate(String value) {
    if (value == null) {
      throw new IllegalArgumentException("The given value must not be null");
    } else if (value.length() > MAX_VALUE_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "The given value exceeds the maximum length of %d: (%d) \"%s\"",
              MAX_VALUE_LENGTH, value.length(), value));
    }
  }

  /**
   * Verifies the given value meets the individual requirements of the defined {@link TagValueType}
   *
   * @param value value to validate
   * @throws IllegalArgumentException if requirements are violated
   */
  @Override
  public void accept(String value) {
    // to be overwritten by individual types
  }
}
