/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Enumeration for the allowed comparison operators used to filter the stored secrets.
 *
 * <p>Supported comparison operators are:
 *
 * <ul>
 *   <li>{@link TagFilterOperator#EQUALS}
 *   <li>{@link TagFilterOperator#LESS_THAN}
 *   <li>{@link TagFilterOperator#GREATER_THAN}
 * </ul>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum TagFilterOperator {
  EQUALS(":"),
  LESS_THAN("<"),
  GREATER_THAN(">");

  /** The string representation of the comparison operator. */
  private final String operator;

  /**
   * Returns the {@link TagFilterOperator} for a given string, matched by the {@link #operator}.<br>
   * This method is case insensitive.
   *
   * @param operator operator to retrieve the {@link TagFilterOperator} for
   * @return the matching {@link TagFilterOperator}
   * @throws IllegalArgumentException if no {@link TagFilterOperator} can be retrieved for the given
   *     operator
   */
  public static TagFilterOperator fromString(String operator) {
    for (TagFilterOperator o : TagFilterOperator.values()) {
      if (o.operator.equalsIgnoreCase(operator)) {
        return o;
      }
    }
    throw new IllegalArgumentException(
        "No TagFilterOperator with value \"" + operator + "\" found");
  }

  @Override
  public String toString() {
    return operator;
  }
}
