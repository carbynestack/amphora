/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.TagFilterOperator.EQUALS;
import static io.carbynestack.amphora.common.TagFilterOperator.LESS_THAN;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.CRITERIA_SEPARATOR;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.service.calculation.OutputDeliveryService;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SecretShareControllerTest {

  @Mock private OutputDeliveryService outputDeliveryService;

  @Mock private StorageService storageService;

  @InjectMocks private SecretShareController secretShareController;

  @SneakyThrows
  @Test
  void
      givenSuccessfulRequestWithoutFilterAndWithoutPaging_whenGetObjectList_thenReturnExpectedContent() {
    List<Metadata> expectedMetadataList = emptyList();
    Page<Metadata> metadataSpringPage = new PageImpl<>(expectedMetadataList, Pageable.unpaged(), 0);
    String filter = null;
    int pageNumber = 0;
    int pageSize = 0;
    String sortProperty = null;
    String sortDirection = null;
    when(storageService.getSecretList(Sort.unsorted())).thenReturn(metadataSpringPage);

    ResponseEntity<MetadataPage> responseEntity =
        secretShareController.getObjectList(
            filter, pageNumber, pageSize, sortProperty, sortDirection);
    MetadataPage actualMetadataPage = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(0, actualMetadataPage.getNumber());
    assertEquals(1, actualMetadataPage.getTotalPages());
    assertEquals(0, actualMetadataPage.getTotalElements());
    assertEquals(expectedMetadataList, actualMetadataPage.getContent());
  }

  @SneakyThrows
  @Test
  void
      givenSuccessfulRequestWithoutFilterButWithPaging_whenGetObjectList_thenReturnExpectedContent() {
    List<Metadata> expectedMetadataList = emptyList();
    Page<Metadata> metadataSpringPage = new PageImpl<>(expectedMetadataList, Pageable.unpaged(), 0);
    String filter = null;
    int pageNumber = 0;
    int pageSize = 1;
    String sortProperty = null;
    String sortDirection = null;
    when(storageService.getSecretList(PageRequest.of(0, 1, Sort.unsorted())))
        .thenReturn(metadataSpringPage);

    ResponseEntity<MetadataPage> responseEntity =
        secretShareController.getObjectList(
            filter, pageNumber, pageSize, sortProperty, sortDirection);
    MetadataPage actualMetadataPage = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(0, actualMetadataPage.getNumber());
    assertEquals(1, actualMetadataPage.getTotalPages());
    assertEquals(0, actualMetadataPage.getTotalElements());
    assertEquals(expectedMetadataList, actualMetadataPage.getContent());
  }

  @SneakyThrows
  @Test
  void
      givenSuccessfulRequestWithFilterAndWithoutPaging_whenGetObjectList_thenReturnExpectedContent() {
    List<Metadata> expectedMetadataList = emptyList();
    Page<Metadata> metadataSpringPage = new PageImpl<>(expectedMetadataList, Pageable.unpaged(), 0);
    String filter = "key" + EQUALS + "value" + CRITERIA_SEPARATOR + "key2" + LESS_THAN + "42";
    List<TagFilter> expectedTagFilter =
        asList(TagFilter.with("key", "value", EQUALS), TagFilter.with("key2", "42", LESS_THAN));
    int pageNumber = 0;
    int pageSize = 0;
    String sortProperty = null;
    String sortDirection = null;
    when(storageService.getSecretList(expectedTagFilter, Sort.unsorted()))
        .thenReturn(metadataSpringPage);

    ResponseEntity<MetadataPage> responseEntity =
        secretShareController.getObjectList(
            filter, pageNumber, pageSize, sortProperty, sortDirection);
    MetadataPage actualMetadataPage = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(0, actualMetadataPage.getNumber());
    assertEquals(1, actualMetadataPage.getTotalPages());
    assertEquals(0, actualMetadataPage.getTotalElements());
    assertEquals(expectedMetadataList, actualMetadataPage.getContent());
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequestWithFilterButWithPaging_whenGetObjectList_thenReturnExpectedContent() {
    List<Metadata> expectedMetadataList = emptyList();
    Page<Metadata> metadataSpringPage = new PageImpl<>(expectedMetadataList, Pageable.unpaged(), 0);
    String filter = "key" + EQUALS + "value" + CRITERIA_SEPARATOR + "key2" + LESS_THAN + "42";
    List<TagFilter> expectedTagFilter =
        asList(TagFilter.with("key", "value", EQUALS), TagFilter.with("key2", "42", LESS_THAN));
    int pageNumber = 0;
    int pageSize = 1;
    String sortProperty = null;
    String sortDirection = null;
    when(storageService.getSecretList(expectedTagFilter, PageRequest.of(0, 1, Sort.unsorted())))
        .thenReturn(metadataSpringPage);

    ResponseEntity<MetadataPage> responseEntity =
        secretShareController.getObjectList(
            filter, pageNumber, pageSize, sortProperty, sortDirection);
    MetadataPage actualMetadataPage = responseEntity.getBody();
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(0, actualMetadataPage.getNumber());
    assertEquals(1, actualMetadataPage.getTotalPages());
    assertEquals(0, actualMetadataPage.getTotalElements());
    assertEquals(expectedMetadataList, actualMetadataPage.getContent());
  }

  @Test
  void givenRequestIdArgumentIsNull_whenGetSecretShare_thenThrowIllegalArgumentException() {
    UUID secretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> secretShareController.getSecretShare(secretId, null));
    assertEquals("Request identifier must not be omitted", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenGetSecretShare_thenReturnOkAndExpectedContent() {
    UUID secretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    UUID requestId = UUID.fromString("d6d0f4ff-df28-4c96-b7df-95170320eaee");
    SecretShare secretShare = SecretShare.builder().secretId(requestId).build();
    OutputDeliveryObject expectedOutputDeliveryObject = mock(OutputDeliveryObject.class);

    when(storageService.getSecretShare(secretId)).thenReturn(secretShare);
    when(outputDeliveryService.computeOutputDeliveryObject(secretShare, requestId))
        .thenReturn(expectedOutputDeliveryObject);
    ResponseEntity<VerifiableSecretShare> responseEntity =
        secretShareController.getSecretShare(secretId, requestId);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(
        VerifiableSecretShare.of(secretShare, expectedOutputDeliveryObject),
        responseEntity.getBody());
  }

  @Test
  void givenSuccessfulRequest_whenDeleteSecretShare_thenReturnOk() {
    UUID secretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    ResponseEntity<Void> actualResponse = secretShareController.deleteSecretShare(secretId);
    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
  }

  @Test
  void givenSortPropertyButInvalidDirection_whenGetSort_thenReturnSortAsc() {
    String expectedProperty = "key";
    String invalidDirection = "invalid";
    assertEquals(
        Sort.by(Sort.Direction.ASC, expectedProperty),
        secretShareController.getSort(expectedProperty, invalidDirection));
  }

  @Test
  void givenNoSortProperty_whenGetSort_thenReturnUnsorted() {
    String emptyProperty = "";
    String direction = Sort.Direction.ASC.toString();
    assertEquals(Sort.unsorted(), secretShareController.getSort(emptyProperty, direction));
  }

  @Test
  void givenValidConfiguration_whenGetSort_thenReturnExpectedContent() {
    String expectedProperty = "key";
    Sort.Direction expectedDirection = Sort.Direction.DESC;
    assertEquals(
        Sort.by(expectedDirection, expectedProperty),
        secretShareController.getSort(expectedProperty, expectedDirection.toString()));
  }
}
