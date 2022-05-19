/*
 * Copyright (c) 2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service;

import io.carbynestack.amphora.common.TagFilter;
import io.vavr.control.Try;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

/** Provides a set of utility methods shared by multiple classes. */
@UtilityClass
public class Utils {
  /**
   * Splits a string around a given separator and parses the single components as {@link TagFilter}.
   *
   * @param in The input string of concatenated {@link TagFilter} separated by a defined character
   * @param separator A String used to separate the individual {@link TagFilter}
   * @return List of extracted {@link TagFilter} if the given filters cannot be decoded
   * @throws UnsupportedEncodingException if the given filters cannot be decoded
   * @throws IllegalArgumentException if the given string cannot be parsed to a valid {@link
   *     TagFilter}.
   */
  public List<TagFilter> parseStringAsTagFilterLists(String in, String separator)
      throws UnsupportedEncodingException {
    List<TagFilter> tagFilters = new ArrayList<>();
    for (String tagFilterString : in.split(separator)) {
      tagFilters.add(TagFilter.fromString(tagFilterString));
    }
    return tagFilters;
  }

  /**
   * Retrieves the {@link Sort} configuration according to the specified properties.
   *
   * <p>Sort direction is {@link Sort#DEFAULT_DIRECTION} by default, if property is {@code null} or
   * cannot be parsed.
   *
   * @param sortProperty the property to sort on
   * @param sortDirection sort direction to apply
   * @return The derived {@link Sort} configuration or {@link Sort#unsorted()} if configuration
   *     cannot be parsed.
   */
  public Sort getSortConfig(String sortProperty, String sortDirection) {
    if (!StringUtils.isEmpty(sortProperty)) {
      return Try.of(() -> Sort.Direction.fromString(sortDirection))
          .map(direction -> Sort.by(direction, sortProperty))
          .getOrElse(Sort.by(sortProperty));
    }
    return Sort.unsorted();
  }
}
