/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * This is a convenience Class used to allow sorting on nested attributes of entities (in a one to
 * many relation) when using the JPA Criteria API.
 *
 * @param <T> the data type of the value used for sorting
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@RequiredArgsConstructor
public class SortableObjectEntity<T> {
  private final SecretEntity secretEntity;
  private T sortValue;
}
