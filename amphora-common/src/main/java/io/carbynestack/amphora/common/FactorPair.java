/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Value;

/**
 * A pair of two factors of type {@link BigInteger} (a, b) to be multiplied with each other. This
 * convenience class is used especially for MPC related operations.
 */
@Value(staticConstructor = "of")
public class FactorPair implements Serializable {
  private static final long serialVersionUID = 4112420116163719023L;

  /** The first factor of this pair */
  BigInteger a;
  /** The second factor of this pair */
  BigInteger b;
}
