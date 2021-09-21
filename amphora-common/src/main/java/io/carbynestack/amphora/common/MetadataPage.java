/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import java.io.Serializable;
import java.util.List;
import lombok.Value;

/**
 * A page containing a list of {@link Metadata} entities as a result of a REST request. This entity
 * is used to handle potentially large result rests by applying pagination mechanisms. <br>
 * A {@link MetadataPage} is not a resource on its own but rather used for representation. It holds
 * the requested content as well as additional information (metadata) about the page itself, e.g.,
 * the size of the page or its number.
 *
 * <p>The client can control a {@link MetadataPage}'s attributes by configuring the query attributes
 * of a GET request accordingly (see {@link io.carbynestack.amphora.common.paging.PageRequest}).
 */
@Value
public class MetadataPage implements Serializable {
  private static final long serialVersionUID = 6723952034915711999L;

  /** The actual requested content provided as a list. */
  List<Metadata> content;
  /** The number of the current {@link MetadataPage}. */
  int number;
  /** The number of {@link Metadata secrets} provided with this {@link MetadataPage}. */
  int size;
  /** The total number of {@link Metadata secrets } matching the request parameters. */
  long totalElements;
  /** The total number of {@link MetadataPage}s available for the given request. */
  int totalPages;
}
