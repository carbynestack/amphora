/*
 * Copyright (c) 2023-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.INTRA_VCP_OPERATIONS_SEGMENT;
import static io.carbynestack.amphora.service.util.ServletUriComponentsBuilderUtil.runInMockedHttpRequestContextForUri;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagValueType;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.opa.JwtReader;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TagsControllerTest {
  private final String authHeader =
      "Bearer"
          + " eyJhbGciOiJSUzI1NiIsImtpZCI6ImM5Njk0OTgyLWQzMTAtNDBkOC04ZDk4LTczOWI1ZGZjNWUyNiIsInR5cCI6IkpXVCJ9.eyJhbXIiOlsicGFzc3dvcmQiXSwiYXRfaGFzaCI6InowbjhudTNJQ19HaXN3bmFTWjgwZ2ciLCJhdWQiOlsiOGExYTIwNzUtMzY3Yi00ZGU1LTgyODgtMGMyNzQ1OTMzMmI3Il0sImF1dGhfdGltZSI6MTczMTUwMDQ0OSwiZXhwIjoxNzMxNTA0NDIyLCJpYXQiOjE3MzE1MDA4MjIsImlzcyI6Imh0dHA6Ly8xNzIuMTguMS4xMjguc3NsaXAuaW8vaWFtL29hdXRoIiwianRpIjoiZTlhMmQxYzQtZGViNy00MTgwLWE0M2YtN2QwNTZhYjNlNTk3Iiwibm9uY2UiOiJnV1JVZjhxTERaeDZpOFNhMXpMdm9IX2tSQ01OWll2WTE0WTFsLWNBU0tVIiwicmF0IjoxNzMxNTAwODIyLCJzaWQiOiJlNGVkOTc2Mi0yMmNlLTQyYzEtOTU3NC01MDVkYjAyMThhNDYiLCJzdWIiOiJhZmMwMTE3Zi1jOWNkLTRkOGMtYWNlZS1mYTE0MzNjYTBmZGQifQ.OACqa6WjpAeZbHR54b3p7saUk9plTdXlZsou41E-gfC7WxCG7ZEKfDPKXUky-r20oeIt1Ov3S2QL6Kefe5dTXEC6nhKGxeClg8ys56_FPcx_neI-p09_pSWOkMx7DHP65giaP7UubyyInVpE-2Eu1o6TpoheahNQfCahKDsJmJ-4Vvts3wA79UMfOI0WHO4vLaaG6DRAZQK_dv7ltw3p_WlncpaQAtHwY9iVhtdB3LtAI39EjplCoAF0c9uQO6W7KHWUlj24l2kc564bsJgZSrYvezw6b2-FY7YisVnicSyAORpeqhWEpLltH3D8I1NtHlSYMJhWuVZbBhAm7Iz6q1-W-Q9ttvdPchdwPSASFRkrjrdIyQf6MsFrItKzUxYck57EYL4GkyN9MWvMNxL1UTtkzGsFEczUVsJFm8OQpulYXIFZksmnPTBB0KuUUvEZ-xih8V1HsMRoHvbiCLaDJwjOFKzPevVggsSMysPKR52UAZJDZLTeHBnVCtQ3rro6T0RxNg94lXypz0AmfsGnoTF34u4FmMxzoeFZ9N5zmEpOnMRqLs7Sb3FnLL-IMitc9_2bsHuFbBJl8KbiGHBQihK5v5SIa292L7P9ChsxomWVhM29qHNFuXQMwFUr57hmveNh2Fz9mduZ5h2hLUuDf5xc6u9rSxy3_e3t_xBuUT4";
  private final String authorizedUserId = "afc0117f-c9cd-4d8c-acee-fa1433ca0fdd";
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
  private final Tag testTag =
      Tag.builder().key("key").value("value").valueType(TagValueType.STRING).build();

  @Mock private StorageService storageService;
  @Mock private JwtReader jwtReader;

  @InjectMocks private TagsController tagsController;

  @Test
  void givenSuccessfulRequest_whenGetTags_thenReturnOkWithExpectedContent()
      throws UnauthorizedException, CsOpaException {
    List<Tag> expectedList = singletonList(testTag);

    when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);
    when(storageService.retrieveTags(testSecretId, authorizedUserId)).thenReturn(expectedList);

    ResponseEntity<List<Tag>> actualResponse = tagsController.getTags(authHeader, testSecretId);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(expectedList, actualResponse.getBody());
  }

  @Test
  void givenTagIsNull_whenCreateTag_thenThrowIllegalArgumentException()
      throws CsOpaException, UnauthorizedException {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> tagsController.createTag(authHeader, testSecretId, null));
    verify(storageService, never()).storeTag(any(), any(), eq(authorizedUserId));
    assertEquals("Tag must not be empty", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenCreateTag_thenReturnCreatedWithExpectedContent()
      throws UnauthorizedException, CsOpaException {
    URI expectedUri =
        URI.create(
            "https://amphora.carbynestack.io" + INTRA_VCP_OPERATIONS_SEGMENT + "/" + testSecretId);

    when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);

    runInMockedHttpRequestContextForUri(
        expectedUri,
        () -> {
          ResponseEntity<URI> actualResponse = null;
          try {
            actualResponse = tagsController.createTag(authHeader, testSecretId, testTag);
          } catch (UnauthorizedException | CsOpaException e) {
            Assertions.fail("unexpected exception thrown: " + e);
          }
          assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
          assertEquals(expectedUri, actualResponse.getBody());
        });

    verify(storageService, times(1)).storeTag(testSecretId, testTag, authorizedUserId);
  }

  @Test
  void givenTagsAreEmpty_whenUpdateTags_thenThrowIllegalArgumentException()
      throws CsOpaException, UnauthorizedException {
    List<Tag> emptyTags = emptyList();
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> tagsController.updateTags(authHeader, testSecretId, emptyTags));
    verify(storageService, never()).replaceTags(any(), any(), eq(authorizedUserId));
    assertEquals("At least one tag must be given.", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenUpdateTags_thenReturnCreatedWithExpectedContent()
      throws UnauthorizedException, CsOpaException {
    List<Tag> newTagList = singletonList(testTag);

    when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);

    ResponseEntity<Void> actualResponse =
        tagsController.updateTags(authHeader, testSecretId, newTagList);
    verify(storageService, times(1)).replaceTags(testSecretId, newTagList, authorizedUserId);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
  }

  @Test
  void givenSuccessfulRequest_whenGetTag_thenReturnOkWithExpectedContent()
      throws UnauthorizedException, CsOpaException {
    when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);
    when(storageService.retrieveTag(testSecretId, testTag.getKey(), authorizedUserId))
        .thenReturn(testTag);

    ResponseEntity<Tag> actualResponse =
        tagsController.getTag(authHeader, testSecretId, testTag.getKey());
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(testTag, actualResponse.getBody());
  }

  @Test
  void givenTagIsNull_whenPutTag_thenTrowIllegalArgumentException()
      throws CsOpaException, UnauthorizedException {
    String key = testTag.getKey();
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> tagsController.putTag(authHeader, testSecretId, key, null));
    verify(storageService, never()).updateTag(testSecretId, testTag, authorizedUserId);
    assertEquals("Tag must not be empty", iae.getMessage());
  }

  @Test
  void givenTagConfigurationDoesNotMatchAddressedKey_whenPutTag_thenTrowIllegalArgumentException()
      throws CsOpaException, UnauthorizedException {
    String nonMatchingKey = testTag.getKey() + "_different";
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> tagsController.putTag(authHeader, testSecretId, nonMatchingKey, testTag));
    verify(storageService, never()).updateTag(testSecretId, testTag, authorizedUserId);
    assertEquals(
        String.format(
            "The defined key and tag data do not match.\n%s <> %s", nonMatchingKey, testTag),
        iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenPutTag_thenReturnOk()
      throws UnauthorizedException, CsOpaException {
    when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);

    ResponseEntity<Void> actualResponse =
        tagsController.putTag(authHeader, testSecretId, testTag.getKey(), testTag);
    verify(storageService, times(1)).updateTag(testSecretId, testTag, authorizedUserId);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
  }

  @Test
  void givenSuccessfulRequest_whenDeleteTag_thenReturnOk()
      throws UnauthorizedException, CsOpaException {
    when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);

    ResponseEntity<Void> actualResponse =
        tagsController.deleteTag(authHeader, testSecretId, testTag.getKey());
    verify(storageService, times(1)).deleteTag(testSecretId, testTag.getKey(), authorizedUserId);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
  }
}
