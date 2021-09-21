/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.exceptions;

/**
 * Exception thrown in case an Amphora Service operation fails.
 *
 * <p>{@link AmphoraServiceException} indicates that internal operations have failed, e.g., when
 * dependencies could not be resolved or are badly configured.
 */
public class AmphoraServiceException extends RuntimeException {

  /**
   * Creates a new {@link AmphoraServiceException} with the given message.
   *
   * @param message The message describing the cause of the exception.
   */
  public AmphoraServiceException(String message) {
    super(message);
  }

  /**
   * Creates a new {@link AmphoraServiceException} with the given message and cause that has led to
   * this exception.
   *
   * @param message The message describing the cause of the exception.
   * @param cause The cause that has lead to this exception providing further details.
   */
  public AmphoraServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
