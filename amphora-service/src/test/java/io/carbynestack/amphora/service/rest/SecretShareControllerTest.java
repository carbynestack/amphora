/*
 * Copyright (c) 2023-2024 - for information on the respective copyright owner
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
import static org.mockito.Mockito.*;

import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.service.calculation.OutputDeliveryService;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.opa.JwtReader;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SecretShareControllerTest {
  private final String authHeader =
      "Bearer"
          + " eyJhbGciOiJSUzI1NiIsImtpZCI6ImM5Njk0OTgyLWQzMTAtNDBkOC04ZDk4LTczOWI1ZGZjNWUyNiIsInR5cCI6IkpXVCJ9.eyJhbXIiOlsicGFzc3dvcmQiXSwiYXRfaGFzaCI6InowbjhudTNJQ19HaXN3bmFTWjgwZ2ciLCJhdWQiOlsiOGExYTIwNzUtMzY3Yi00ZGU1LTgyODgtMGMyNzQ1OTMzMmI3Il0sImF1dGhfdGltZSI6MTczMTUwMDQ0OSwiZXhwIjoxNzMxNTA0NDIyLCJpYXQiOjE3MzE1MDA4MjIsImlzcyI6Imh0dHA6Ly8xNzIuMTguMS4xMjguc3NsaXAuaW8vaWFtL29hdXRoIiwianRpIjoiZTlhMmQxYzQtZGViNy00MTgwLWE0M2YtN2QwNTZhYjNlNTk3Iiwibm9uY2UiOiJnV1JVZjhxTERaeDZpOFNhMXpMdm9IX2tSQ01OWll2WTE0WTFsLWNBU0tVIiwicmF0IjoxNzMxNTAwODIyLCJzaWQiOiJlNGVkOTc2Mi0yMmNlLTQyYzEtOTU3NC01MDVkYjAyMThhNDYiLCJzdWIiOiJhZmMwMTE3Zi1jOWNkLTRkOGMtYWNlZS1mYTE0MzNjYTBmZGQifQ.OACqa6WjpAeZbHR54b3p7saUk9plTdXlZsou41E-gfC7WxCG7ZEKfDPKXUky-r20oeIt1Ov3S2QL6Kefe5dTXEC6nhKGxeClg8ys56_FPcx_neI-p09_pSWOkMx7DHP65giaP7UubyyInVpE-2Eu1o6TpoheahNQfCahKDsJmJ-4Vvts3wA79UMfOI0WHO4vLaaG6DRAZQK_dv7ltw3p_WlncpaQAtHwY9iVhtdB3LtAI39EjplCoAF0c9uQO6W7KHWUlj24l2kc564bsJgZSrYvezw6b2-FY7YisVnicSyAORpeqhWEpLltH3D8I1NtHlSYMJhWuVZbBhAm7Iz6q1-W-Q9ttvdPchdwPSASFRkrjrdIyQf6MsFrItKzUxYck57EYL4GkyN9MWvMNxL1UTtkzGsFEczUVsJFm8OQpulYXIFZksmnPTBB0KuUUvEZ-xih8V1HsMRoHvbiCLaDJwjOFKzPevVggsSMysPKR52UAZJDZLTeHBnVCtQ3rro6T0RxNg94lXypz0AmfsGnoTF34u4FmMxzoeFZ9N5zmEpOnMRqLs7Sb3FnLL-IMitc9_2bsHuFbBJl8KbiGHBQihK5v5SIa292L7P9ChsxomWVhM29qHNFuXQMwFUr57hmveNh2Fz9mduZ5h2hLUuDf5xc6u9rSxy3_e3t_xBuUT4";
  private final String authorizedUserId = "afc0117f-c9cd-4d8c-acee-fa1433ca0fdd";

  private final OutputDeliveryService outputDeliveryService = mock(OutputDeliveryService.class);
  private final StorageService storageService = mock(StorageService.class);
  private final JwtReader jwtReader = mock(JwtReader.class);

  private final SecretShareController secretShareController =
      new SecretShareController(storageService, outputDeliveryService, jwtReader);

  @BeforeEach
  void setUp() throws UnauthorizedException {
    when(jwtReader.extractUserIdFromAuthHeader(authHeader)).thenReturn(authorizedUserId);
    reset(storageService, outputDeliveryService);
  }

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
            () -> secretShareController.getSecretShare(authHeader, secretId, null));
    assertEquals("Request identifier must not be omitted", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenGetSecretShare_thenReturnOkAndExpectedContent()
      throws CsOpaException, UnauthorizedException {
    UUID secretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    UUID requestId = UUID.fromString("d6d0f4ff-df28-4c96-b7df-95170320eaee");
    SecretShare secretShare = SecretShare.builder().secretId(requestId).build();
    OutputDeliveryObject expectedOutputDeliveryObject = mock(OutputDeliveryObject.class);

    when(storageService.getSecretShare(secretId, authorizedUserId)).thenReturn(secretShare);
    when(outputDeliveryService.computeOutputDeliveryObject(secretShare, requestId))
        .thenReturn(expectedOutputDeliveryObject);

    ResponseEntity<VerifiableSecretShare> responseEntity =
        secretShareController.getSecretShare(authHeader, secretId, requestId);
    verify(jwtReader, times(1)).extractUserIdFromAuthHeader(authHeader);
    verify(storageService, times(1)).getSecretShare(secretId, authorizedUserId);
    verify(outputDeliveryService, times(1)).computeOutputDeliveryObject(secretShare, requestId);
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    assertEquals(
        VerifiableSecretShare.of(secretShare, expectedOutputDeliveryObject),
        responseEntity.getBody());
  }

  @Test
  void givenSuccessfulRequest_whenDeleteSecretShare_thenReturnOk()
      throws UnauthorizedException, CsOpaException {
    UUID secretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    ResponseEntity<Void> actualResponse =
        secretShareController.deleteSecretShare(authHeader, secretId);
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
