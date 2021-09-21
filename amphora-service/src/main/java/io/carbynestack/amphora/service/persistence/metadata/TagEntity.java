/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import static org.springframework.util.StringUtils.isEmpty;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagValueType;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.CollectionUtils;

@Entity
@Table(name = TagEntity.TABLE_NAME)
@Data
@Accessors(chain = true)
@EntityListeners(AuditingEntityListener.class)
public class TagEntity implements Serializable {

  @Transient public static final String TABLE_NAME = "secret_tags";

  @Transient public static final String ID_FIELD = "id";

  @Transient public static final String SECRET_FIELD = "secret";

  @Transient public static final String KEY_FIELD = "key";

  @Transient public static final String VALUE_FIELD = "value";

  @Transient public static final String VALUE_TYPE_FIELD = "valueType";

  @Transient
  // Don't use key since this is a reserved word in sql syntax
  public static final String KEY_COLUMN = "tag_" + KEY_FIELD;

  @Transient
  // Don't use value since this is a reserved word in sql syntax
  public static final String VALUE_COLUMN = "tag_" + VALUE_FIELD;

  @Transient public static final String VALUE_TYPE_COLUMN = "tag_" + VALUE_TYPE_FIELD;

  @Column(name = ID_FIELD)
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;

  @ManyToOne
  @JoinColumn(name = SECRET_FIELD, nullable = false)
  private SecretEntity secret;

  @Column(name = KEY_COLUMN)
  private String key;

  @Column(name = VALUE_COLUMN)
  private String value;

  @Column(name = VALUE_TYPE_COLUMN)
  private String valueType;

  public Tag toTag() {
    return Tag.builder()
        .key(this.getKey())
        .value(this.getValue())
        .valueType(TagValueType.valueOf(this.getValueType()))
        .build();
  }

  public static List<Tag> setToTagList(Set<TagEntity> tagEntities) {
    if (CollectionUtils.isEmpty(tagEntities)) {
      return new ArrayList<>();
    }

    return tagEntities.stream().map(TagEntity::toTag).collect(Collectors.toList());
  }

  public static TagEntity fromTag(Tag tag) {
    return new TagEntity()
        .setKey(tag.getKey())
        .setValue(tag.getValue())
        .setValueType(tag.getValueType().name());
  }

  public static Set<TagEntity> setFromTagList(List<Tag> tags) {
    if (CollectionUtils.isEmpty(tags)) {
      return Collections.emptySet();
    }
    return tags.stream()
        .filter(tag -> !isEmpty(tag.getKey()))
        .map(TagEntity::fromTag)
        .collect(Collectors.toSet());
  }

  // Overwrite required to break toString loop -> secret prints SecretEntity.getSecretId() only
  @Override
  public String toString() {
    return "TagEntity{"
        + "id="
        + id
        + ", secret="
        + (secret == null ? null : secret.getSecretId())
        + ", key='"
        + key
        + '\''
        + ", value='"
        + value
        + '\''
        + ", valueType='"
        + valueType
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TagEntity tagEntity = (TagEntity) o;
    return id == tagEntity.id
        && ((secret == null && tagEntity.secret == null)
            || (secret != null
                && tagEntity.secret != null
                && Objects.equals(secret.getSecretId(), tagEntity.getSecret().getSecretId())))
        && Objects.equals(key, tagEntity.key)
        && Objects.equals(value, tagEntity.value)
        && Objects.equals(valueType, tagEntity.valueType);
  }
}
