/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.Tag;
import java.util.*;
import org.junit.jupiter.api.Test;

public class MetadataTest {

  @Test
  void givenIdIsNull_whenCallingBuildOnBuilder_thenThrowException() {
    Metadata.MetadataBuilder<?, ?> builder = Metadata.builder();
    NullPointerException actualNpe = assertThrows(NullPointerException.class, builder::build);
    assertThat(actualNpe.getMessage()).startsWith("secretId").contains("is null");
  }

  @Test
  void givenListWithNullTags_whenSettingTagsOnBuilder_thenRemoveNullTagsFromList() {
    UUID secretId = UUID.fromString("80fbba1b-3da8-4b1e-8a2c-cebd65229fad");
    List<Tag> expectedTags =
        Arrays.asList(Tag.builder().key("key1").build(), Tag.builder().key("key2").build());
    List<Tag> tagListWithNullEntries = new ArrayList<>(expectedTags);
    tagListWithNullEntries.add(0, null);
    tagListWithNullEntries.add(tagListWithNullEntries.size(), null);
    Metadata metadata =
        Metadata.builder()
            .secretId(secretId)
            .tags(Collections.unmodifiableList(tagListWithNullEntries))
            .build();
    assertEquals(secretId, metadata.getSecretId());
    assertThat(metadata.getTags()).containsOnly(expectedTags.toArray(new Tag[0]));
    assertEquals(metadata.getTags().size(), expectedTags.size());
    assertThat(metadata.getTags()).isInstanceOf(ArrayList.class);
  }
}
