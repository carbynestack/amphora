/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpaResultTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void givenResultIsTrue_whenParsed_thenIsAllowedShouldBeTrue() throws JsonProcessingException {
        OpaResult result = objectMapper.readValue("{\"result\": true}", OpaResult.class);
        assertTrue(result.isAllowed(), "isAllowed must be true");
    }

    @Test
    void givenResultIsEmpty_whenParsed_thenIsAllowedShouldBeFalse() throws JsonProcessingException {
        OpaResult result = objectMapper.readValue("{}", OpaResult.class);
        assertFalse(result.isAllowed(), "isAllowed must be false");
    }
}