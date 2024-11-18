/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtReaderTest {

    @Test
    void givenTokenProvided_whenExtractSubject_thenReturnSubject() throws UnauthorizedException {
        JwtReader jwtReader = new JwtReader("sub");
        String header = "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImM5Njk0OTgyLWQzMTAtNDBkOC04ZDk4LTczOWI1ZGZjNWUyNiIsInR5cCI6IkpXVCJ9.eyJhbXIiOlsicGFzc3dvcmQiXSwiYXRfaGFzaCI6InowbjhudTNJQ19HaXN3bmFTWjgwZ2ciLCJhdWQiOlsiOGExYTIwNzUtMzY3Yi00ZGU1LTgyODgtMGMyNzQ1OTMzMmI3Il0sImF1dGhfdGltZSI6MTczMTUwMDQ0OSwiZXhwIjoxNzMxNTA0NDIyLCJpYXQiOjE3MzE1MDA4MjIsImlzcyI6Imh0dHA6Ly8xNzIuMTguMS4xMjguc3NsaXAuaW8vaWFtL29hdXRoIiwianRpIjoiZTlhMmQxYzQtZGViNy00MTgwLWE0M2YtN2QwNTZhYjNlNTk3Iiwibm9uY2UiOiJnV1JVZjhxTERaeDZpOFNhMXpMdm9IX2tSQ01OWll2WTE0WTFsLWNBU0tVIiwicmF0IjoxNzMxNTAwODIyLCJzaWQiOiJlNGVkOTc2Mi0yMmNlLTQyYzEtOTU3NC01MDVkYjAyMThhNDYiLCJzdWIiOiJhZmMwMTE3Zi1jOWNkLTRkOGMtYWNlZS1mYTE0MzNjYTBmZGQifQ.OACqa6WjpAeZbHR54b3p7saUk9plTdXlZsou41E-gfC7WxCG7ZEKfDPKXUky-r20oeIt1Ov3S2QL6Kefe5dTXEC6nhKGxeClg8ys56_FPcx_neI-p09_pSWOkMx7DHP65giaP7UubyyInVpE-2Eu1o6TpoheahNQfCahKDsJmJ-4Vvts3wA79UMfOI0WHO4vLaaG6DRAZQK_dv7ltw3p_WlncpaQAtHwY9iVhtdB3LtAI39EjplCoAF0c9uQO6W7KHWUlj24l2kc564bsJgZSrYvezw6b2-FY7YisVnicSyAORpeqhWEpLltH3D8I1NtHlSYMJhWuVZbBhAm7Iz6q1-W-Q9ttvdPchdwPSASFRkrjrdIyQf6MsFrItKzUxYck57EYL4GkyN9MWvMNxL1UTtkzGsFEczUVsJFm8OQpulYXIFZksmnPTBB0KuUUvEZ-xih8V1HsMRoHvbiCLaDJwjOFKzPevVggsSMysPKR52UAZJDZLTeHBnVCtQ3rro6T0RxNg94lXypz0AmfsGnoTF34u4FmMxzoeFZ9N5zmEpOnMRqLs7Sb3FnLL-IMitc9_2bsHuFbBJl8KbiGHBQihK5v5SIa292L7P9ChsxomWVhM29qHNFuXQMwFUr57hmveNh2Fz9mduZ5h2hLUuDf5xc6u9rSxy3_e3t_xBuUT4";
        String result = jwtReader.extractUserIdFromAuthHeader(header);
        String expectedSubject = "afc0117f-c9cd-4d8c-acee-fa1433ca0fdd";
        assertEquals(expectedSubject, result);
    }

    @Test
    void givenNoTokenProvided_whenExtractSubject_thenThrowUnauthorizedException() throws UnauthorizedException {
        JwtReader jwtReader = new JwtReader("sub");
        String invalidToken = "{\"auth_time\": 1731500449,\n" +
                "  \"exp\": 1731504422,\n" +
                "  \"iat\": 1731500822,\n" +
                "  \"something\": {\n" +
                "    \"what\": \"is this\"\n"+
                "  }}";
        assertThrows(UnauthorizedException.class, () -> jwtReader.extractUserIdFromAuthHeader(
                String.format(
                        "Bearer header.%s.signature",
                        Base64.toBase64String(invalidToken.getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    void givenJwtOfInvalidFormat_whenExtractSubject_thenThrowUnauthorizedException() {
        JwtReader jwtReader = new JwtReader("sub");
        assertThrows(UnauthorizedException.class, () ->
                jwtReader.extractUserIdFromAuthHeader("Bearer invalid.jwt_missing_dot_token"));
    }

    @Test
    void givenJwtDataFieldOfInvalidFormat_whenExtractSubject_thenThrowUnauthorizedException() {
        JwtReader jwtReader = new JwtReader("sub");
        String invalidDataJson = "{";
        assertThrows(UnauthorizedException.class, () ->
                jwtReader.extractUserIdFromAuthHeader(
                        String.format("header.%s.signature", Base64.toBase64String(invalidDataJson.getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    void givenInvalidAuthHeader_whenExtractSubject_thenThrowUnauthorizedException() {
        JwtReader jwtReader = new JwtReader("sub");
        assertThrows(UnauthorizedException.class, () ->
                jwtReader.extractUserIdFromAuthHeader("invalid_auth_header missing Bearer prefix"));
    }
}