/*
 * Copyright (c) 2021-2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common.exceptions;

/**
 * An exception thrown in case the verification of a received shared data fails.
 *
 * <p>This can occur if either data has been falsified during transmission due to technical errors
 * or if one of the MPC parties has behaved dishonest.<br>
 * The operation can possibly be repeated to narrow down the source of the error.
 */
public class IntegrityVerificationException extends RuntimeException {
  /**
   * Creates a new {@link IntegrityVerificationException} with the given message and cause that has
   * led to this exception.
   *
   * @param message The message describing the cause of the exception.
   */
  public IntegrityVerificationException(String message) {
    super(message);
  }
}
