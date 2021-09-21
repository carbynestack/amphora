/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Value;

/**
 * A dedicated object used to share a single party's interim values from a MPC multiplication with
 * the other participating players of the MPC cluster.
 */
@Value
public class MultiplicationExchangeObject implements Serializable {
  private static final long serialVersionUID = -2506050297585267011L;

  /**
   * Unique identifier used to identify the shared information and link it to (a) specific
   * multiplication(s) performed distributed amongst the involved players.
   */
  UUID operationId;
  /**
   * The id of the player in the mpc cluster to whom the shared information belongs to. <br>
   * This information is especially required to ensure that the interim values of all players have
   * actually been shared and received successfully.
   */
  int playerId;
  /**
   * The actual interim value(s) of the performed multiplication(s). <br>
   * Each entry in the list represents the interim data of a single MPC-based multiplication.
   */
  List<FactorPair> interimValues;
}
