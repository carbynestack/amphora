/*
 * Copyright (c) 2023-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import io.carbynestack.amphora.common.MaskedInput;
import io.carbynestack.amphora.common.MaskedInputData;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.opa.JwtReader;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.UUID;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.INTRA_VCP_OPERATIONS_SEGMENT;
import static io.carbynestack.amphora.service.util.ServletUriComponentsBuilderUtil.runInMockedHttpRequestContextForUri;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaskedInputControllerTest {
  private final String authHeader = "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6ImM5Njk0OTgyLWQzMTAtNDBkOC04ZDk4LTczOWI1ZGZjNWUyNiIsInR5cCI6IkpXVCJ9.eyJhbXIiOlsicGFzc3dvcmQiXSwiYXRfaGFzaCI6InowbjhudTNJQ19HaXN3bmFTWjgwZ2ciLCJhdWQiOlsiOGExYTIwNzUtMzY3Yi00ZGU1LTgyODgtMGMyNzQ1OTMzMmI3Il0sImF1dGhfdGltZSI6MTczMTUwMDQ0OSwiZXhwIjoxNzMxNTA0NDIyLCJpYXQiOjE3MzE1MDA4MjIsImlzcyI6Imh0dHA6Ly8xNzIuMTguMS4xMjguc3NsaXAuaW8vaWFtL29hdXRoIiwianRpIjoiZTlhMmQxYzQtZGViNy00MTgwLWE0M2YtN2QwNTZhYjNlNTk3Iiwibm9uY2UiOiJnV1JVZjhxTERaeDZpOFNhMXpMdm9IX2tSQ01OWll2WTE0WTFsLWNBU0tVIiwicmF0IjoxNzMxNTAwODIyLCJzaWQiOiJlNGVkOTc2Mi0yMmNlLTQyYzEtOTU3NC01MDVkYjAyMThhNDYiLCJzdWIiOiJhZmMwMTE3Zi1jOWNkLTRkOGMtYWNlZS1mYTE0MzNjYTBmZGQifQ.OACqa6WjpAeZbHR54b3p7saUk9plTdXlZsou41E-gfC7WxCG7ZEKfDPKXUky-r20oeIt1Ov3S2QL6Kefe5dTXEC6nhKGxeClg8ys56_FPcx_neI-p09_pSWOkMx7DHP65giaP7UubyyInVpE-2Eu1o6TpoheahNQfCahKDsJmJ-4Vvts3wA79UMfOI0WHO4vLaaG6DRAZQK_dv7ltw3p_WlncpaQAtHwY9iVhtdB3LtAI39EjplCoAF0c9uQO6W7KHWUlj24l2kc564bsJgZSrYvezw6b2-FY7YisVnicSyAORpeqhWEpLltH3D8I1NtHlSYMJhWuVZbBhAm7Iz6q1-W-Q9ttvdPchdwPSASFRkrjrdIyQf6MsFrItKzUxYck57EYL4GkyN9MWvMNxL1UTtkzGsFEczUVsJFm8OQpulYXIFZksmnPTBB0KuUUvEZ-xih8V1HsMRoHvbiCLaDJwjOFKzPevVggsSMysPKR52UAZJDZLTeHBnVCtQ3rro6T0RxNg94lXypz0AmfsGnoTF34u4FmMxzoeFZ9N5zmEpOnMRqLs7Sb3FnLL-IMitc9_2bsHuFbBJl8KbiGHBQihK5v5SIa292L7P9ChsxomWVhM29qHNFuXQMwFUr57hmveNh2Fz9mduZ5h2hLUuDf5xc6u9rSxy3_e3t_xBuUT4";
  private final String authorizedUserId ="afc0117f-c9cd-4d8c-acee-fa1433ca0fdd";

  private final StorageService storageService = mock(StorageService.class);
  private final JwtReader jwtReader = mock(JwtReader.class);

  private final MaskedInputController maskedInputController = new MaskedInputController(storageService, jwtReader);

  @BeforeEach
  public void setUp() throws UnauthorizedException {
        when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);
  }

  @Test
  void givenArgumentIsNull_whenUpload_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> maskedInputController.upload(authHeader, null));
    assertEquals("MaskedInput must not be null", iae.getMessage());
  }

  @Test
  void givenMaskedInputDataIsEmpty_whenUpload_thenThrowIllegalArgumentException() {
    UUID expectedId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    MaskedInput maskedInput = new MaskedInput(expectedId, emptyList(), emptyList());

    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> maskedInputController.upload(authHeader, maskedInput));
    assertEquals("MaskedInput data must not be empty", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenUpload_thenReturnCreatedWithExpectedContent() {
    UUID secretShareId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    URI expectedUri =
        URI.create(
            "https://amphora.carbynestack.io" + INTRA_VCP_OPERATIONS_SEGMENT + "/" + secretShareId);
    MaskedInput maskedInput =
        new MaskedInput(
            secretShareId, singletonList(MaskedInputData.of(new byte[16])), emptyList());

    when(storageService.createSecret(maskedInput, authorizedUserId)).thenReturn(secretShareId.toString());

    runInMockedHttpRequestContextForUri(
        expectedUri,
        () -> {
            ResponseEntity<URI> actualResponse = null;
            try {
                actualResponse = maskedInputController.upload(authHeader, maskedInput);
            } catch (UnauthorizedException e) {
                fail("Unexpected exception thrown", e);
            }

            assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
          assertEquals(expectedUri, actualResponse.getBody());
        });
  }
}
