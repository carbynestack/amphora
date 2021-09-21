/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.exceptions;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;

public class AmphoraClientExceptionTest {
  @Test
  public void givenMessageAndCause_whenInstantiatingException_thenSetValuesCorrectly() {
    String msg = "Description";
    String causeMsg = "Root of all evil";
    Throwable cause = new NullPointerException(causeMsg);
    AmphoraClientException amphoraClientException = new AmphoraClientException(msg, cause);
    assertEquals(msg, amphoraClientException.getMessage());
    assertEquals(cause, amphoraClientException.getCause());
    assertEquals(Collections.emptyMap(), amphoraClientException.getUriStatusMap());
  }
}
