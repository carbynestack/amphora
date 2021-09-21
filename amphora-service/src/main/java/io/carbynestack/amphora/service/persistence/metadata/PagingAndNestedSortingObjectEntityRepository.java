/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.metadata;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Interface providing paging and sorting operations for {@link SecretEntity} related database
 * operations.
 */
interface PagingAndNestedSortingObjectEntityRepository {
  /**
   * Returns a Page of {@link SecretEntity} matching the given paging configuration.
   *
   * @param pageable the paging configuration
   * @return a page of {@link SecretEntity SecretEntities}
   */
  @Transactional(readOnly = true)
  Page<SecretEntity> findAll(@NonNull Pageable pageable);

  /**
   * Retrieves {@link SecretEntity}s matching the given specification and return them as a {@link
   * Page} according given paging configuration.
   *
   * @param spec filtering specification
   * @param pageable the paging configuration
   * @return a page of {@link SecretEntity ObjectEntities}
   */
  @Transactional(readOnly = true)
  Page<SecretEntity> findAll(@NonNull SecretEntitySpecification spec, @NonNull Pageable pageable);
}
