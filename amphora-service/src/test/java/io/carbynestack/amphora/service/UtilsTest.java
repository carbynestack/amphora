/*
 * Copyright (c) 2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service;

import static io.carbynestack.amphora.common.TagFilter.ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG;
import static org.junit.Assert.assertEquals;

import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.TagFilterOperator;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Sort;

public class UtilsTest {
  @Test
  public void
      givenConcatenatedTagFilterString_whenParseStringAsTagFilterLists_thenReturnExpectedList()
          throws UnsupportedEncodingException {
    String separator = ";";
    String filter1String = "key1" + TagFilterOperator.LESS_THAN + "123";
    String filter2String = "key2" + TagFilterOperator.EQUALS + "value2";
    String filterListString = filter1String + separator + filter2String;

    assertEquals(
        Arrays.asList(TagFilter.fromString(filter1String), TagFilter.fromString(filter2String)),
        Utils.parseStringAsTagFilterLists(filterListString, separator));
  }

  @Test
  public void givenInvalidTagFilterString_whenParseStringAsTagFilterLists_thenThrowException() {
    String separator = ";";
    String filter1String = "key1" + TagFilterOperator.LESS_THAN + "123";
    String invalidFilterString = "key!=value";
    String filterListString = filter1String + separator + invalidFilterString;

    IllegalArgumentException actualIAE =
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> Utils.parseStringAsTagFilterLists(filterListString, separator));
    assertEquals(
        String.format(ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG, invalidFilterString),
        actualIAE.getMessage());
  }

  @Test
  public void givenSortPropertyButInvalidDirection_whenGetSort_thenReturnSortAsc() {
    String expectedProperty = "key";
    String invalidDirection = "invalid";
    assertEquals(
        Sort.by(Sort.Direction.ASC, expectedProperty),
        Utils.getSortConfig(expectedProperty, invalidDirection));
  }

  @Test
  public void givenNoSortProperty_whenGetSort_thenReturnUnsorted() {
    String emptyProperty = "";
    String direction = Sort.Direction.ASC.toString();
    assertEquals(Sort.unsorted(), Utils.getSortConfig(emptyProperty, direction));
  }

  @Test
  public void givenValidConfiguration_whenGetSort_thenReturnExpectedContent() {
    String expectedProperty = "key";
    Sort.Direction expectedDirection = Sort.Direction.DESC;
    assertEquals(
        Sort.by(expectedDirection, expectedProperty),
        Utils.getSortConfig(expectedProperty, expectedDirection.toString()));
  }
}
