/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import java.util.UUID;

/** Client interface for all MPC party internal Service-to-Service operations. */
public interface AmphoraIntraVcpClient {

  /**
   * Stores an {@link SecretShare} in amphora.
   *
   * @param secretShare The {@link SecretShare} to be stored
   * @return The id of the {@link SecretShare} as referenced by amphora
   * @throws IllegalArgumentException If the secret's id is omitted
   */
  UUID uploadSecretShare(SecretShare secretShare) throws AmphoraClientException;

  /**
   * Downloads a single {@link SecretShare} by a given id
   *
   * @param secretId The id of the {@link SecretShare}
   * @throws IllegalArgumentException If the secretId is omitted
   */
  SecretShare getSecretShare(UUID secretId) throws AmphoraClientException;
}
