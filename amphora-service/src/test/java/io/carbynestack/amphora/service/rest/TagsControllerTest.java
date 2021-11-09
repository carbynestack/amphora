/*
 * Copyright (c) 2021 - for information on the respective copyright owner
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
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class TagsControllerTest {
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
  private final Tag testTag =
      Tag.builder().key("key").value("value").valueType(TagValueType.STRING).build();

  @Mock private StorageService storageService;

  @InjectMocks private TagsController tagsController;

  @Test
  void givenSuccessfulRequest_whenGetTags_thenReturnOkWithExpectedContent() {
    List<Tag> expectedList = singletonList(testTag);

    when(storageService.retrieveTags(testSecretId)).thenReturn(expectedList);

    ResponseEntity<List<Tag>> actualResponse = tagsController.getTags(testSecretId);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(expectedList, actualResponse.getBody());
  }

  @Test
  void givenTagIsNull_whenCreateTag_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> tagsController.createTag(testSecretId, null));
    verify(storageService, never()).storeTag(any(), any());
    assertEquals("Tag must not be empty", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenCreateTag_thenReturnCreatedWithExpectedContent() {
    URI expectedUri =
        URI.create(
            "https://amphora.carbynestack.io" + INTRA_VCP_OPERATIONS_SEGMENT + "/" + testSecretId);
    runInMockedHttpRequestContextForUri(
        expectedUri,
        () -> {
          ResponseEntity<URI> actualResponse = tagsController.createTag(testSecretId, testTag);
          verify(storageService, times(1)).storeTag(testSecretId, testTag);
          assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
          assertEquals(expectedUri, actualResponse.getBody());
        });
  }

  @Test
  void givenTagsAreEmpty_whenUpdateTags_thenThrowIllegalArgumentException() {
    List<Tag> emptyTags = emptyList();
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> tagsController.updateTags(testSecretId, emptyTags));
    verify(storageService, never()).replaceTags(any(), any());
    assertEquals("At least one tag must be given.", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenUpdateTags_thenReturnCreatedWithExpectedContent() {
    List<Tag> newTagList = singletonList(testTag);
    ResponseEntity<Void> actualResponse = tagsController.updateTags(testSecretId, newTagList);
    verify(storageService, times(1)).replaceTags(testSecretId, newTagList);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
  }

  @Test
  void givenSuccessfulRequest_whenGetTag_thenReturnOkWithExpectedContent() {
    when(storageService.retrieveTag(testSecretId, testTag.getKey())).thenReturn(testTag);

    ResponseEntity<Tag> actualResponse = tagsController.getTag(testSecretId, testTag.getKey());
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(testTag, actualResponse.getBody());
  }

  @Test
  void givenTagIsNull_whenPutTag_thenTrowIllegalArgumentException() {
    String key = testTag.getKey();
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> tagsController.putTag(testSecretId, key, null));
    verify(storageService, never()).updateTag(testSecretId, testTag);
    assertEquals("Tag must not be empty", iae.getMessage());
  }

  @Test
  void givenTagConfigurationDoesNotMatchAddressedKey_whenPutTag_thenTrowIllegalArgumentException() {
    String nonMatchingKey = testTag.getKey() + "_different";
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> tagsController.putTag(testSecretId, nonMatchingKey, testTag));
    verify(storageService, never()).updateTag(testSecretId, testTag);
    assertEquals(
        String.format(
            "The defined key and tag data do not match.\n%s <> %s", nonMatchingKey, testTag),
        iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenPutTag_thenReturnOk() {
    ResponseEntity<Void> actualResponse =
        tagsController.putTag(testSecretId, testTag.getKey(), testTag);
    verify(storageService, times(1)).updateTag(testSecretId, testTag);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
  }

  @Test
  void givenSuccessfulRequest_whenDeleteTag_thenReturnOk() {
    ResponseEntity<Void> actualResponse = tagsController.deleteTag(testSecretId, testTag.getKey());
    verify(storageService, times(1)).deleteTag(testSecretId, testTag.getKey());
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
  }
}
