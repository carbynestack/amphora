/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common.paging;

import java.io.Serializable;
import lombok.*;

/**
 * A {@link PageRequest} describes a client request to sort and slice a result set according to the
 * given configuration and retrieve a specific page.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Generated
public class PageRequest implements Serializable {
  private static final long serialVersionUID = 8951257523502908930L;

  /** The requested page of the sliced result set. */
  @Builder.Default int page = 0;
  /**
   * The number of items that should be returned.
   *
   * <p>The total result set will be sliced according to this value.
   */
  @Builder.Default int size = Integer.MAX_VALUE;
  /** The configuration for the sorting of the items in the result set. */
  Sort sort;
}
