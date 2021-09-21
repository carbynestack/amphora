/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;

/** Client interface for all MPC-Cluster internal Amphora-Service-to-Amphora-Service operations. */
public interface AmphoraInterVcpClient {

  /**
   * Broadcasts an {@link MultiplicationExchangeObject} to all partners of the CS MPC cluster.
   *
   * @param multiplicationExchangeObject The {@link SecretShare} to be shared
   * @throws IllegalArgumentException If the {@link MultiplicationExchangeObject#getOperationId()}
   *     is omitted
   * @throws AmphoraClientException When could not be shared with all partners successfully
   */
  void open(MultiplicationExchangeObject multiplicationExchangeObject)
      throws AmphoraClientException;
}
