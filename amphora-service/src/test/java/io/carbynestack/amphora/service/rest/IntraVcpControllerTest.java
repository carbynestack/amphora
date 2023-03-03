/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.INTRA_VCP_OPERATIONS_SEGMENT;
import static io.carbynestack.amphora.service.util.ServletUriComponentsBuilderUtil.runInMockedHttpRequestContextForUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class IntraVcpControllerTest {

  @Mock private StorageService storageService;

  @InjectMocks private IntraVcpController intraVcpController;

  @Test
  void givenArgumentIsNull_whenUploadSecretShare_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> intraVcpController.uploadSecretShare(null));
    assertEquals("SecretShare must not be null", iae.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenUploadSecretShare_thenReturnCreatedWithExpectedContent() {
    UUID secretShareId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    URI expectedUri =
        URI.create(
            "https://amphora.carbynestack.io" + INTRA_VCP_OPERATIONS_SEGMENT + "/" + secretShareId);
    SecretShare secretShare = SecretShare.builder().secretId(secretShareId).build();

    when(storageService.storeSecretShare(secretShare)).thenReturn(secretShareId.toString());

    runInMockedHttpRequestContextForUri(
        expectedUri,
        () -> {
          ResponseEntity<URI> actualResponse = intraVcpController.uploadSecretShare(secretShare);
          assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
          assertEquals(expectedUri, actualResponse.getBody());
        });
  }

  @Test
  void givenSuccessfulRequest_whenDownloadSecretShare_thenReturnOkWithExpectedContent() {
    UUID secretShareId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    SecretShare expectedSecretShare = SecretShare.builder().secretId(secretShareId).build();

    when(storageService.getSecretShare(secretShareId)).thenReturn(expectedSecretShare);

    ResponseEntity<SecretShare> actualResponse =
        intraVcpController.downloadSecretShare(secretShareId);

    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(expectedSecretShare, actualResponse.getBody());
  }
}
