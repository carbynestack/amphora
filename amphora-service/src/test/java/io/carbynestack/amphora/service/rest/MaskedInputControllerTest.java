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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import io.carbynestack.amphora.common.MaskedInput;
import io.carbynestack.amphora.common.MaskedInputData;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.net.URI;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class MaskedInputControllerTest {

  @Mock private StorageService storageService;

  @InjectMocks private MaskedInputController maskedInputController;

  @Test
  public void givenArgumentIsNull_whenUpload_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> maskedInputController.upload(null));
    assertEquals("MaskedInput must not be null", iae.getMessage());
  }

  @Test
  public void givenMaskedInputDataIsEmpty_whenUpload_thenThrowIllegalArgumentException() {
    UUID expectedId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    MaskedInput maskedInput = new MaskedInput(expectedId, emptyList(), emptyList());

    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> maskedInputController.upload(maskedInput));
    assertEquals("MaskedInput data must not be empty", iae.getMessage());
  }

  @Test
  public void givenSuccessfulRequest_whenUpload_thenReturnCreatedWithExpectedContent() {
    UUID secretShareId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    URI expectedUri =
        URI.create(
            "https://amphora.carbynestack.io" + INTRA_VCP_OPERATIONS_SEGMENT + "/" + secretShareId);
    MaskedInput maskedInput =
        new MaskedInput(
            secretShareId, singletonList(MaskedInputData.of(new byte[16])), emptyList());

    when(storageService.createSecret(maskedInput)).thenReturn(secretShareId.toString());

    runInMockedHttpRequestContextForUri(
        expectedUri,
        () -> {
          ResponseEntity<URI> actualResponse = maskedInputController.upload(maskedInput);

          assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
          assertEquals(expectedUri, actualResponse.getBody());
        });
  }
}
