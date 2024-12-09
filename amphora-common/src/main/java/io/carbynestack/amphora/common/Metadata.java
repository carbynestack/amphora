/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import java.io.Serializable;
import java.util.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * The Metadata is used to describe the stored secret shares. <br>
 * Each {@link Metadata} entity is identified by its {@link Metadata#secretId} which is unique for a
 * single party in the virtual cloud, but shared across the participating parties in order to match
 * all related shares of a secret. <br>
 * The secret's {@link Metadata#tags} which are used to describe the stored content but also to keep
 * general information like the time of creation are, stored as a list. These information stored in
 * clear, shared and identical across all parties of the virtual cloud.
 *
 * <p>Metadata is used for example when the list of stored secrets is queried. There the actual
 * stored data is not of interest, but general stored information which describes the stored
 * content.
 *
 * <p>New {@link Metadata} objects are created using the {@link Metadata#builder() builder}.
 */
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Jacksonized
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class Metadata implements Serializable {
  private static final long serialVersionUID = 9087948790172766148L;

  /**
   * Identifier of the stored secret. <br>
   * Unique for a single party in the virtual cloud, but shared across the participating parties in
   * order to match all related shares of a secret.
   */
  @NonNull UUID secretId;

  /**
   * A list of {@link Tag}s used to describe the stored content. <br>
   * These tags are stored in clear, shared and identical across all parties of the virtual cloud.
   */
  List<Tag> tags;

  public Optional<Tag> getTagByKey(String key) {
    return tags.stream().filter(tag -> tag.getKey().equals(key)).findFirst();
  }

  /**
   * A Builder for {@link Metadata}.
   *
   * <p>A new {@link MetadataBuilder} can be created using {@link Metadata#builder()}. Each
   * attribute can be modified using the individual setter method.
   *
   * <p>This class is filled by <i>lombok</i> with its builder logic. Only {@link
   * MetadataBuilder#tags(List)} is pre-defined with custom logic.
   *
   * @param <C>
   * @param <B>
   */
  public abstract static class MetadataBuilder<
      C extends Metadata, B extends MetadataBuilder<C, B>> {
    /** A list of {@link Tag}s used to describe the {@link Metadata secret} */
    List<Tag> tags;

    /**
     * Sets the tags used to describe the secret. (see {@link Metadata#tags})
     *
     * <p>The tags will internally be stored as an {@link ArrayList} and <i>null</i>-objects will be
     * removed from the list before storing the data.
     *
     * @param tags the {@link Tag}s used to describe the secret.
     * @return this {@link MetadataBuilder builder}
     */
    public MetadataBuilder<C, B> tags(List<Tag> tags) {
      if (tags != null && !tags.isEmpty()) {
        tags = new ArrayList<>(tags);
        tags.removeIf(Objects::isNull);
      }
      this.tags = tags;
      return this;
    }
  }
}
