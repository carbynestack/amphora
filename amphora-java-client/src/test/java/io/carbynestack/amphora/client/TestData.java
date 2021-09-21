/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.MetadataPage;
import io.carbynestack.amphora.common.Tag;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestData {
  static Secret getObject(BigInteger[] data) {
    return Secret.of(getTags(), data);
  }

  static MetadataPage getObjectMetadataPage() {
    return new MetadataPage(getObjectMetadataList(), 0, 2, 2, 1);
  }

  private static List<Metadata> getObjectMetadataList() {
    List<Metadata> metadataList = new ArrayList<>();
    metadataList.add(
        Metadata.builder()
            .secretId(UUID.fromString("52bd51d2-f73d-404a-a326-3af9e76b6c38"))
            .tags(getTags())
            .build());
    metadataList.add(
        Metadata.builder()
            .secretId(UUID.fromString("03ad388b-0ea2-44ef-98a3-a4cb2e686dfa"))
            .tags(getTags())
            .build());
    return metadataList;
  }

  static List<Tag> getTags() {
    Tag tag1 = Tag.builder().key("key1").value("tag1").build();
    Tag tag2 = Tag.builder().key("key2").value("tag2").build();
    List<Tag> tags = new ArrayList<>();
    tags.add(tag1);
    tags.add(tag2);
    return tags;
  }
}
