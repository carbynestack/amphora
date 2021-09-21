/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.exceptions;

/** Exception thrown in case of a ID conflict when creating a new secret. */
public class AlreadyExistsException extends RuntimeException {
  /**
   * Creates a new exception with the given message.
   *
   * @param msg The message describing the cause of the exception.
   */
  public AlreadyExistsException(String msg) {
    super(msg);
  }
}
