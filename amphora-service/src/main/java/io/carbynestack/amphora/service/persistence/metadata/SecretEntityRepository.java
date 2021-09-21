/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interface providing CRUD, paging and sorting operations for {@link SecretEntity} related database
 * operations.
 */
public interface SecretEntityRepository
    extends JpaRepository<SecretEntity, String>, PagingAndNestedSortingObjectEntityRepository {

  /**
   * Deletes an {@link SecretEntity} with the given id.
   *
   * @param secretId id of the {@link SecretEntity}
   * @return number of actual deleted entities. <i>0</i> if no entity was deleted.
   */
  @Transactional
  long deleteBySecretId(String secretId);
}
