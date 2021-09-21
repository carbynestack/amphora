/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common.paging;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Value;

/**
 * Configuration for the sorting of query result sets.
 *
 * <p>New {@link Sort} configurations are created using the static constructor {@link
 * Sort#by(String, Order)}
 */
@Value
@Generated
@AllArgsConstructor(staticName = "by")
public class Sort implements Serializable {
  private static final long serialVersionUID = 6718543268474742232L;

  /** Enumeration for the sorting directions. */
  public enum Order {
    ASC,
    DESC
  }

  /** The property to sort for */
  String property;
  /** The sort direction */
  Order order;
}
