/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import java.util.function.Function;

/**
 * Functional interface for providing a bearer token based on some input value.
 *
 * @param <T> the type of the input from which a bearer token is derived
 */
public interface BearerTokenProvider<T> extends Function<T, String> {}
