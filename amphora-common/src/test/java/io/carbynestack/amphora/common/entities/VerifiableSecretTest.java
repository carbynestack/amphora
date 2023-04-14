/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.VerifiableSecretShare;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class VerifiableSecretTest {
  private OutputDeliveryObject testOutputDeliveryObject =
      OutputDeliveryObject.builder()
          .secretShares("sShares".getBytes())
          .rShares("rShares".getBytes())
          .vShares("vShares".getBytes())
          .wShares("wShares".getBytes())
          .uShares("uShares".getBytes())
          .build();
  Tag testTag = Tag.builder().key("carbyne").value("stack").build();
  Metadata testMetadata =
      Metadata.builder()
          .secretId(UUID.fromString("80fbba1b-3da8-4b1e-8a2c-cebd65229fad"))
          .tags(Collections.singletonList(testTag))
          .build();
  private String expectedJsonString =
      "{\n"
          + "  \"secretId\" : \""
          + testMetadata.getSecretId()
          + "\",\n"
          + "  \"tags\" : [ {\n"
          + "    \"key\" : \""
          + testTag.getKey()
          + "\",\n"
          + "    \"value\" : \""
          + testTag.getValue()
          + "\",\n"
          + "    \"valueType\" : \""
          + testTag.getValueType()
          + "\"\n"
          + "  } ],\n"
          + "  \"secretShares\" : \""
          + Base64.encode(testOutputDeliveryObject.getSecretShares())
          + "\",\n"
          + "  \"rShares\" : \""
          + Base64.encode(testOutputDeliveryObject.getRShares())
          + "\",\n"
          + "  \"vShares\" : \""
          + Base64.encode(testOutputDeliveryObject.getVShares())
          + "\",\n"
          + "  \"wShares\" : \""
          + Base64.encode(testOutputDeliveryObject.getWShares())
          + "\",\n"
          + "  \"uShares\" : \""
          + Base64.encode(testOutputDeliveryObject.getUShares())
          + "\"\n"
          + "}";
  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void givenValidJsonString_whenDeserialize_thenReturnExpectedObject()
      throws JsonProcessingException {
    VerifiableSecretShare vss =
        objectMapper.readValue(expectedJsonString, VerifiableSecretShare.class);
    assertEquals(testOutputDeliveryObject, vss.getOutputDeliveryObject());
    assertEquals(testMetadata, vss.getMetadata());
  }

  @Test
  public void givenVssObject_whenSerialize_thenReturnExpectedJsonString()
      throws JsonProcessingException {
    VerifiableSecretShare vss = VerifiableSecretShare.of(testMetadata, testOutputDeliveryObject);
    assertEquals(
        expectedJsonString, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vss));
  }

  @Test
  public void
      givenJsonMissingOutputDeliveryObjectAttribute_whenDeserialize_thenReturnThrowException()
          throws JsonProcessingException {
    String invalidJson = expectedJsonString.replaceFirst("\"rShares\" :\\s\"\\S+\",", "");
    ValueInstantiationException vie =
        assertThrows(
            ValueInstantiationException.class,
            () -> objectMapper.readValue(invalidJson, VerifiableSecretShare.class));
    assertThat(vie.getMessage()).contains("rShares is marked non-null but is null");
  }

  @Test
  public void givenJsonMissingMetadataAttribute_whenDeserialize_thenReturnThrowException()
      throws JsonProcessingException {
    String invalidJson = expectedJsonString.replaceFirst("\"secretId\" :\\s\"\\S+\",", "");
    ValueInstantiationException vie =
        assertThrows(
            ValueInstantiationException.class,
            () -> objectMapper.readValue(invalidJson, VerifiableSecretShare.class));
    assertThat(vie.getMessage()).contains("secretId is marked non-null but is null");
  }
}
