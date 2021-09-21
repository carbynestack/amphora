/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.client;

import io.vavr.control.Try;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {
  static <T> Map<URI, Try<T>> wrap(Map<URI, T> in) {
    return in.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Try.success(e.getValue())));
  }
}
