/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static io.carbynestack.amphora.common.Tag.INVALID_KEY_STRING_EXCEPTION_MSG;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.TagFilterOperator;
import io.carbynestack.amphora.common.TagValueType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TagTest {
  private final String dummyKey = "dummy-key";
  private final String dummyStringValue = "dummy-value";
  private final long dummyLongValue = 12345L;

  @Test
  void givenKeyWithTooManyCharacters_whenBuildingTag_thenThrowIllegalArgumentException() {
    char[] array = new char[129];
    Arrays.fill(array, 'a');
    String key = new String(array);
    Tag.TagBuilder tagBuilder = Tag.builder().key(key).value(dummyStringValue);
    IllegalArgumentException actualIae =
        assertThrows(IllegalArgumentException.class, () -> tagBuilder.build());
    assertThat(actualIae.getMessage(), containsString("longer then 128 characters"));
  }

  @Test
  void givenKeyWithMaximumLength_whenBuildingTag_thenSucceed() {
    char[] array = new char[128];
    Arrays.fill(array, 'a');
    String key = new String(array);
    Tag tag = Tag.builder().key(key).value(dummyStringValue).build();
    assertNotNull(tag);
  }

  @Test
  void givenKeyWithInvalidCharacter_whenBuildingTag_thenThrowIllegalArgumentException() {
    String key = "#123";
    Tag.TagBuilder tagBuilder = Tag.builder().key(key).value(dummyStringValue);
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> tagBuilder.build());
    assertEquals(INVALID_KEY_STRING_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  void givenNullAsKey_whenBuildingTag_thenThrowIllegalArgumentException() {
    Tag.TagBuilder tagBuilder = Tag.builder();
    NullPointerException actualNpe =
        assertThrows(NullPointerException.class, () -> tagBuilder.key(null));
    assertEquals("key is marked non-null but is null", actualNpe.getMessage());
  }

  @Test
  void giveKeyIsEmptyString_whenBuildingTag_thenThrowIllegalArgumentException() {
    Tag.TagBuilder tagBuilder = Tag.builder().key("").value(dummyStringValue);
    IllegalArgumentException actualIae =
        assertThrows(IllegalArgumentException.class, tagBuilder::build);
    assertEquals(INVALID_KEY_STRING_EXCEPTION_MSG, actualIae.getMessage());
  }

  @Test
  void givenNullAsValue_whenBuildingTag_thenThrowIllegalArgumentException() {
    Tag.TagBuilder tagBuilder = Tag.builder().key(dummyKey);
    NullPointerException actualNpe =
        assertThrows(NullPointerException.class, () -> tagBuilder.value(null));
    assertEquals("value is marked non-null but is null", actualNpe.getMessage());
  }

  @Test
  void givenValueIsEmptyString_whenBuildingTag_thenCreateExpectedTag() {
    String expectedTagValue = "";
    Tag tag = Tag.builder().key(dummyKey).value("").build();
    assertEquals(TagValueType.STRING, tag.getValueType());
    assertEquals(expectedTagValue, tag.getValue());
    assertEquals(dummyKey, tag.getKey());
  }

  @Test
  void givenKeyWithSupportedCharacters_whenBuildingTag_thenSucceed() {
    String expectedKey = "abcDEF456-.";
    Tag tag = Tag.builder().key(expectedKey).value(dummyStringValue).build();
    assertEquals(TagValueType.STRING, tag.getValueType());
    assertEquals(expectedKey, tag.getKey());
    assertEquals(dummyStringValue, tag.getValue());
  }

  @Test
  void givenValueWithTooManyCharacters_whenBuildingTag_thenThrowIllegaArgumentException() {
    char[] array = new char[257];
    Arrays.fill(array, 'a');
    String value = new String(array);
    Tag.TagBuilder tagBuilder = Tag.builder().key(dummyKey).value(value);
    IllegalArgumentException actualIae =
        assertThrows(IllegalArgumentException.class, tagBuilder::build);
    assertThat(
        actualIae.getMessage(),
        startsWith("The given value exceeds the maximum length of 256: (257)"));
  }

  @Test
  void givenValueWithMaximumLength_whenBuildingTag_thenSucceed() {
    char[] array = new char[256];
    Arrays.fill(array, 'a');
    String expectedValue = new String(array);
    Tag tag = Tag.builder().key(dummyKey).value(expectedValue).build();
    assertEquals(TagValueType.STRING, tag.getValueType());
    assertEquals(dummyKey, tag.getKey());
    assertEquals(expectedValue, tag.getValue());
  }

  @Test
  void givenLongStringAsValueAndLongAsValueType_whenBuildingTag_thenCreateExpectedTag() {
    Tag tag =
        Tag.builder()
            .key(dummyKey)
            .value(String.valueOf(dummyLongValue))
            .valueType(TagValueType.LONG)
            .build();
    assertEquals(TagValueType.LONG, tag.getValueType());
    assertEquals(dummyKey, tag.getKey());
    assertEquals(Long.toString(dummyLongValue), tag.getValue());
  }

  @Test
  void givenOnlyKeyAsInput_whenBuildingTag_thenUseDefaultValues() {
    Tag tag = Tag.builder().key(dummyKey).build();
    assertEquals(TagValueType.STRING, tag.getValueType());
    assertEquals(dummyKey, tag.getKey());
    assertEquals("", tag.getValue());
  }

  @Test
  void givenTagObject_whenSerializingAndDeserializingData_thenRecreateOrigin()
      throws JsonProcessingException {
    Tag tag =
        Tag.builder()
            .key(dummyKey)
            .value(Long.toString(dummyLongValue))
            .valueType(TagValueType.LONG)
            .build();
    ObjectMapper om = new ObjectMapper();
    assertEquals(tag, om.readerFor(Tag.class).readValue(om.writeValueAsString(tag)));
  }

  @Test
  void givenTagFilterObject_whenSerializingAndDeserializingData_thenRecreateOrigin()
      throws JsonProcessingException {
    TagFilter filter =
        TagFilter.with(dummyKey, Long.toString(dummyLongValue), TagFilterOperator.EQUALS);
    ObjectMapper om = new ObjectMapper();
    assertEquals(filter, om.readerFor(TagFilter.class).readValue(om.writeValueAsString(filter)));
  }
}
