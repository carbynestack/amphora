/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.CRITERIA_SEPARATOR;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.INTRA_VCP_OPERATIONS_SEGMENT;
import static io.carbynestack.amphora.service.util.ServletUriComponentsBuilderUtil.runInMockedHttpRequestContextForUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.TagFilterOperator;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class IntraVcpControllerTest {

  @Mock private StorageService storageService;

  @InjectMocks private IntraVcpController intraVcpController;

  @Test
  public void givenArgumentIsNull_whenUploadSecretShare_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> intraVcpController.uploadSecretShare(null));
    assertEquals("SecretShare must not be null", iae.getMessage());
  }

  @Test
  public void givenSuccessfulRequest_whenUploadSecretShare_thenReturnCreatedWithExpectedContent() {
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
  public void givenSuccessfulRequest_whenDownloadSecretShare_thenReturnOkWithExpectedContent() {
    UUID secretShareId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
    SecretShare expectedSecretShare = SecretShare.builder().secretId(secretShareId).build();

    when(storageService.getSecretShare(secretShareId)).thenReturn(expectedSecretShare);

    ResponseEntity<SecretShare> actualResponse =
        intraVcpController.downloadSecretShare(secretShareId);

    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(expectedSecretShare, actualResponse.getBody());
  }

  @Test
  public void givenSuccessfulRequest_whenGetSecretShareDataList_thenReturnOkWithExpectedContent()
      throws UnsupportedEncodingException {
    String sortProperty = "key";
    Sort.Direction sortDirection = Sort.Direction.DESC;
    String filter1String = "key1" + TagFilterOperator.LESS_THAN + "123";
    String filter2String = "key2" + TagFilterOperator.EQUALS + "value2";
    String filterListString = filter1String + CRITERIA_SEPARATOR + filter2String;

    List<byte[]> expectedSecretShareDataList =
        Arrays.asList(RandomUtils.nextBytes(42), RandomUtils.nextBytes(30));
    List<TagFilter> expectedTagFilterList =
        Arrays.asList(TagFilter.fromString(filter1String), TagFilter.fromString(filter2String));
    Sort expectedSort = Sort.by(sortDirection, sortProperty);

    when(storageService.getSecretShareDataList(expectedTagFilterList, expectedSort))
        .thenReturn(expectedSecretShareDataList);

    ResponseEntity<List<byte[]>> actualResponse =
        intraVcpController.getSecretShareDataList(
            filterListString, sortProperty, sortDirection.toString());

    assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    assertEquals(expectedSecretShareDataList, actualResponse.getBody());
  }
}
