/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.vavr.control.Option;

import java.util.Base64;

public class JwtReader {
    static ObjectMapper mapper = new ObjectMapper();
    private final String userIdField;

    public JwtReader(String userIdField) {
        this.userIdField = userIdField;
    }

    public String extractUserIdFromAuthHeader(String header) throws UnauthorizedException {
        return extractFieldFromAuthHeader(header, userIdField);
    }

    private static String extractFieldFromAuthHeader(String header, String field) throws UnauthorizedException {
        return tokenFromHeader(header)
                .flatMap(JwtReader::dataNodeFromToken)
                .flatMap(node -> fieldFromNode(node, field))
                .getOrElseThrow(() -> new UnauthorizedException("No token provided"));
    }

    private static Option<JsonNode> dataNodeFromToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Option.none();
        }
        try {
            String jwt = new String(Base64.getDecoder().decode(parts[1]));
            return Option.of(mapper.reader().readTree(jwt));
        } catch (JsonProcessingException e) {
            return Option.none();
        }
    }

    private static Option<String> tokenFromHeader(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return Option.of(header.substring(7));
        }
        return Option.none();
    }

    private static Option<String> fieldFromNode(JsonNode node, String fieldName) {
        JsonNode field = node;
        try {
            for(String f : fieldName.split("\\.")) {
                field = field.get(f);
            }
            return Option.of(field.asText());
        } catch (NullPointerException e) {
            return Option.none();
        }
    }
}

