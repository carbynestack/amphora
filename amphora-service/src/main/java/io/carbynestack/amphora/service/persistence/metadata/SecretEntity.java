/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import io.carbynestack.amphora.client.Secret;
import io.carbynestack.amphora.common.Metadata;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Describes the database persistence {@link Entity} for {@link Secret}. */
@Entity
@Table(name = SecretEntity.TABLE_NAME)
@EntityListeners(AuditingEntityListener.class)
@Data
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecretEntity implements Serializable {
  private static final long serialVersionUID = 7692392374150517666L;

  @Transient public static final String TABLE_NAME = "amphora_secrets";
  @Transient public static final String SECRET_ID_COLUMN = "secret_id";
  @Transient public static final String SECRET_ID_FIELD = "secretId";

  @Column(name = SECRET_ID_COLUMN)
  @Id
  private String secretId;

  @Setter(value = AccessLevel.PRIVATE)
  @OneToMany(
      targetEntity = TagEntity.class,
      mappedBy = TagEntity.SECRET_FIELD,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<TagEntity> tags;

  /**
   * Creates a new {@link SecretEntity} with the given id and tags.
   *
   * <p>The new {@link SecretEntity} will automatically assign itself to the given {@link TagEntity
   * tags} as reference secret.
   *
   * @param secretId the id of the created {@link SecretEntity}
   * @param tags the tags of the created {@link SecretEntity}
   */
  public SecretEntity(@NonNull String secretId, @NonNull Set<TagEntity> tags) {
    this.setSecretId(secretId);
    this.assignTags(tags);
  }

  /**
   * Sets the tag of this {@link SecretEntity}
   *
   * <p>This {@link SecretEntity} will automatically assign itself to the given {@link TagEntity
   * tags} as reference secret.
   *
   * @param tags the new tags of this {@link SecretEntity}
   * @return this {@link SecretEntity}
   */
  public SecretEntity assignTags(Set<TagEntity> tags) {
    tags.forEach(t -> t.setSecret(this));
    this.tags = tags;
    return this;
  }

  public Metadata toMetadata() {
    return Metadata.builder()
        .secretId(UUID.fromString(this.secretId))
        .tags(TagEntity.setToTagList(this.tags))
        .build();
  }
}
