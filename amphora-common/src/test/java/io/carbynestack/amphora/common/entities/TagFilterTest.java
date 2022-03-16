package io.carbynestack.amphora.common.entities;

import static io.carbynestack.amphora.common.TagFilter.ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.TagFilterOperator;
import java.io.UnsupportedEncodingException;
import org.junit.Test;

public class TagFilterTest {
  @Test
  public void givenTagFilterStringWithoutKey_whenFromString_thenThrowExpectedException() {
    String invalidTagFilterString = TagFilterOperator.EQUALS + "42";

    IllegalArgumentException actualIAE =
        assertThrows(
            IllegalArgumentException.class, () -> TagFilter.fromString(invalidTagFilterString));

    assertEquals(
        String.format(ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG, invalidTagFilterString),
        actualIAE.getMessage());
  }

  @Test
  public void givenTagFilterStringWithInvalidOperator_whenFromString_thenThrowExpectedException() {
    String invalidTagFilterString = "key<>Value";

    IllegalArgumentException actualIAE =
        assertThrows(
            IllegalArgumentException.class, () -> TagFilter.fromString(invalidTagFilterString));

    assertEquals(
        String.format(ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG, invalidTagFilterString),
        actualIAE.getMessage());
  }

  @Test
  public void givenTagFilterStringWithoutValue_whenFromString_thenThrowExpectedException() {
    String invalidTagFilterString = "key" + TagFilterOperator.EQUALS;

    IllegalArgumentException actualIAE =
        assertThrows(
            IllegalArgumentException.class, () -> TagFilter.fromString(invalidTagFilterString));

    assertEquals(
        String.format(ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG, invalidTagFilterString),
        actualIAE.getMessage());
  }

  @Test
  public void givenValidTagFilterString_whenFromString_thenReturnExpectedTagFilter()
      throws UnsupportedEncodingException {
    TagFilter expectedTagFilter = TagFilter.with("time", "42", TagFilterOperator.LESS_THAN);
    String validTagFilterString =
        expectedTagFilter.getKey() + expectedTagFilter.getOperator() + expectedTagFilter.getValue();

    TagFilter actualTagFilter = TagFilter.fromString(validTagFilterString);

    assertEquals(expectedTagFilter, actualTagFilter);
  }
}
