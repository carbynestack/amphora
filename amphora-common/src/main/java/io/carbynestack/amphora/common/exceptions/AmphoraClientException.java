/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common.exceptions;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * Exception thrown in case a client triggered operation fails.
 *
 * <p>{@link AmphoraClientException} are expected to be thrown on invalid client interactions, such
 * as malformed input or invalid requests, rather than service internal errors.
 *
 * <p>For HTTP based operation, {@link #getUriStatusMap()} may provide additional details about
 * which request(s) have failed. This map can contain individual HTTP status codes per <code>URI
 * </code> that was accessed. a request.
 */
public class AmphoraClientException extends IOException implements Serializable {
  private static final long serialVersionUID = 9061774459401318039L;
  private final Map<URI, Integer> uriStatusMap;

  /**
   * Creates a new exception with the given message and an empty status map.
   *
   * @param message The message describing the cause of the exception.
   */
  public AmphoraClientException(String message) {
    this(message, Collections.emptyMap());
  }

  /**
   * Creates a new exception with the given message and status map.
   *
   * @param message The message describing the cause of the exception.
   * @param uriStatusMap The status map that contains a Http status code per <code>URI</code> that
   *     returned a request.
   */
  public AmphoraClientException(String message, Map<URI, Integer> uriStatusMap) {
    super(message);
    this.uriStatusMap = uriStatusMap;
  }

  /**
   * Creates a new exception with the given message and cause that has led to this {@link
   * AmphoraServiceException}.
   *
   * @param message The message describing the cause of the exception.
   * @param cause The cause that has lead to this exception providing further details.
   */
  public AmphoraClientException(String message, Throwable cause) {
    super(message, cause);
    this.uriStatusMap = Collections.emptyMap();
  }

  /**
   * Returns an map containing a Http status code for each individual call to the backend services.
   *
   * @return The status map.
   */
  public Map<URI, Integer> getUriStatusMap() {
    return uriStatusMap;
  }
}
