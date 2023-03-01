/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common;

import static io.carbynestack.amphora.common.AmphoraServiceUri.INVALID_SERVICE_ADDRESS_EXCEPTION_MSG;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class AmphoraServiceUriTest {

  @Test
  void givenNullAsServiceAddress_whenCreatingAmphoraServiceUri_thenThrowIllegalArgumentException() {
    IllegalArgumentException expectedException =
        assertThrows(IllegalArgumentException.class, () -> new AmphoraServiceUri(null));
    assertEquals("serviceAddress must not be empty!", expectedException.getMessage());
  }

  @Test
  void
      givenEmptyStringAsServiceAddress_whenCreatingAmphoraServiceUri_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> new AmphoraServiceUri(""));
    assertEquals("serviceAddress must not be empty!", iae.getMessage());
  }

  @Test
  void givenNoSchemeDefined_whenCreatingAmphoraServiceUri_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> new AmphoraServiceUri("localhost:8080"));
    assertEquals(INVALID_SERVICE_ADDRESS_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  void givenInvalidUriString_whenCreatingAmphoraServiceUri_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> new AmphoraServiceUri("invalidUri"));
    assertEquals(INVALID_SERVICE_ADDRESS_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  void
      givenUriStringWithDomain_whenCreatingAmphoraServiceUri_thenCreateExpectedAmphoraServiceUri() {
    AmphoraServiceUri aUri = new AmphoraServiceUri("https://amphora.carbynestack.io:8080");
    assertEquals("https://amphora.carbynestack.io:8080", aUri.getServiceUri().toString());
    assertEquals("amphora.carbynestack.io", aUri.getServiceUri().getHost());
    assertEquals(8080, aUri.getServiceUri().getPort());
  }

  @Test
  void
      givenUriStringWithIpAndPort_whenCreatingAmphoraServiceUri_thenCreateExpectedAmphoraServiceUri() {
    AmphoraServiceUri aUri = new AmphoraServiceUri("http://127.0.0.1:8080");
    assertEquals("http://127.0.0.1:8080", aUri.getServiceUri().toString());
    assertEquals("127.0.0.1", aUri.getServiceUri().getHost());
    assertEquals(8080, aUri.getServiceUri().getPort());
  }

  @Test
  void
      givenUriStringWithoutPort_whenCreatingAmphoraServiceUri_thenCreateExpectedAmphoraServiceUri() {
    AmphoraServiceUri aUri = new AmphoraServiceUri("https://amphora.carbynestack.io");
    assertEquals("https://amphora.carbynestack.io", aUri.getServiceUri().toString());
    assertEquals("amphora.carbynestack.io", aUri.getServiceUri().getHost());
  }

  @Test
  void givenUriStringWithPath_whenCreatingAmphoraServiceUri_thenCreateExpectedAmphoraServiceUri() {
    AmphoraServiceUri aUri = new AmphoraServiceUri("https://amphora.carbynestack.io/myService");
    assertEquals("https://amphora.carbynestack.io/myService", aUri.getServiceUri().toString());
    assertEquals("amphora.carbynestack.io", aUri.getServiceUri().getHost());
    assertEquals("/myService", aUri.getServiceUri().getPath());
  }

  @Test
  void
      givenUriStringWithPortAndPath_whenCreatingAmphoraServiceUri_thenCreateExpectedAmphoraServiceUri() {
    AmphoraServiceUri aUri =
        new AmphoraServiceUri("https://amphora.carbynestack.io:8081/my/service");
    assertEquals(
        "https://amphora.carbynestack.io:8081/my/service", aUri.getServiceUri().toString());
    assertEquals("amphora.carbynestack.io", aUri.getServiceUri().getHost());
    assertEquals(8081, aUri.getServiceUri().getPort());
    assertEquals("/my/service", aUri.getServiceUri().getPath());
  }

  @Test
  void givenAmphoraServiceUri_whenGetSecretShareUri_thenReturnExpectedUri() {
    AmphoraServiceUri aUri =
        new AmphoraServiceUri("https://amphora.carbynestack.io:8081/my/service");
    URI secretShareUri = aUri.getSecretShareUri();
    assertEquals(
        String.format("https://amphora.carbynestack.io:8081/my/service%s", SECRET_SHARES_ENDPOINT),
        secretShareUri.toString());
    assertEquals("amphora.carbynestack.io", secretShareUri.getHost());
    assertEquals(8081, secretShareUri.getPort());
    assertEquals(String.format("/my/service%s", SECRET_SHARES_ENDPOINT), secretShareUri.getPath());
  }

  @Test
  void givenAmphoraServiceUri_whenGetMaskedInputUri_thenReturnExpectedUri() {
    AmphoraServiceUri aUri =
        new AmphoraServiceUri("https://amphora.carbynestack.io:8081/my/service");
    URI maskedInputUri = aUri.getMaskedInputUri();
    assertEquals(
        String.format(
            "https://amphora.carbynestack.io:8081/my/service%s", UPLOAD_MASKED_INPUTS_ENDPOINT),
        maskedInputUri.toString());
    assertEquals("amphora.carbynestack.io", maskedInputUri.getHost());
    assertEquals(8081, maskedInputUri.getPort());
    assertEquals(
        String.format("/my/service%s", UPLOAD_MASKED_INPUTS_ENDPOINT), maskedInputUri.getPath());
  }

  @Test
  void givenAmphoraServiceUri_whenGetInputMaskUri_thenReturnExpectedUri() {
    AmphoraServiceUri aUri =
        new AmphoraServiceUri("https://amphora.carbynestack.io:8081/my/service/");
    URI inputMaskUri = aUri.getInputMaskUri();
    assertEquals(
        String.format(
            "https://amphora.carbynestack.io:8081/my/service%s", DOWNLOAD_INPUT_MASKS_ENDPOINT),
        inputMaskUri.toString());
    assertEquals("amphora.carbynestack.io", inputMaskUri.getHost());
    assertEquals(8081, inputMaskUri.getPort());
    assertEquals(
        String.format("/my/service%s", DOWNLOAD_INPUT_MASKS_ENDPOINT), inputMaskUri.getPath());
  }

  @Test
  void
      givenUriStringWithTrailingSlash_whenCreateAmphoraServiceUri_thenReturnExpectedAmphoraServiceUri() {
    AmphoraServiceUri aUri = new AmphoraServiceUri("https://amphora.carbynestack.io:8081/");
    URI inputMaskUri = aUri.getInputMaskUri();
    assertEquals(
        String.format("https://amphora.carbynestack.io:8081%s", DOWNLOAD_INPUT_MASKS_ENDPOINT),
        inputMaskUri.toString());
    assertEquals("amphora.carbynestack.io", inputMaskUri.getHost());
    assertEquals(8081, inputMaskUri.getPort());
    assertEquals(String.format("%s", DOWNLOAD_INPUT_MASKS_ENDPOINT), inputMaskUri.getPath());
  }

  @SneakyThrows
  @Test
  void givenValidPathSegments_whenBuildingResourceUri_thenReturnExpectedUri() {
    String baseUri = "https://amphora.carbynestack.io/amphora";
    String pathVariable = "1234";
    AmphoraServiceUri amphoraServiceUri = new AmphoraServiceUri(baseUri);
    assertEquals(
        String.format("%s/%s", baseUri, pathVariable),
        amphoraServiceUri.attachPathParameter(new URI(baseUri), pathVariable).toString());
  }
}
