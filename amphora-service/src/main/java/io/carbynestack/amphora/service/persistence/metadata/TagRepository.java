/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface TagRepository extends CrudRepository<TagEntity, Integer> {
  @Transactional(readOnly = true)
  Optional<TagEntity> findBySecretAndKey(@NonNull SecretEntity secret, @NonNull String key);

  @Transactional
  long deleteBySecret(@NonNull SecretEntity secret);
}
