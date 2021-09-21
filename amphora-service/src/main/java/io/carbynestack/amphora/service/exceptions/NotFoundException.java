/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.exceptions;

/** Exception thrown in case a requested entity does not exist. */
public class NotFoundException extends RuntimeException {
  /**
   * Creates a new exception with the given message.
   *
   * @param msg The message describing the cause of the exception.
   */
  public NotFoundException(String msg) {
    super(msg);
  }
}
