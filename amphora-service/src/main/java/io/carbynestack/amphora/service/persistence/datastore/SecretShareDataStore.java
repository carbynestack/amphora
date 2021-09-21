/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.datastore;

import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import java.util.UUID;

/**
 * A persistence layer to store a secret's actual data (secret-shares).
 *
 * <p>The data of a secret is stored as byte[] referenced by the secret's unique ID represented by a
 * {@link java.util.UUID}.
 *
 * <p>The <i>SecretShareDataStore</i> interface provides three basic methods to create, retrieve and
 * delete the data. The data of a secret is considered immutable and therefore no update or put
 * method is provided.
 */
public interface SecretShareDataStore {

  /**
   * Stores a SecretShare's data in the store.
   *
   * @param id Unique identifier for the secret to which the data belongs
   * @param data The secret's data to store
   * @throws AmphoraServiceException if storing fails
   */
  void storeSecretShareData(UUID id, byte[] data);

  /**
   * Gets a SecretShare's data from the store.
   *
   * @param id identifies the Secret Share to get
   * @return the data for the SecretShare with the given id
   * @throws AmphoraServiceException if retrieving the given secret fails
   */
  byte[] getSecretShareData(UUID id);

  /**
   * Deletes a SecretShare's from the store and returns the data that was associate with the
   * specified id.
   *
   * @param id identifies the SecretShare which data should be deleted
   * @return The formerly stored data
   * @throws AmphoraServiceException if deleting fails
   */
  byte[] deleteSecretShareData(UUID id);
}
