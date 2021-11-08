/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static io.carbynestack.amphora.common.SecretShare.INVALID_LENGTH_EXCEPTION_MSG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.util.*;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

class SecretShareTest {
  final Random random = new Random(42);
  final UUID testSecretId = UUID.fromString("80fbba1b-3da8-4b1e-8a2c-cebd65229fad");

  @Test
  void givenDataOfInvalidLength_whenBuildSecretShare_thenThrowIllegalArgumentException() {
    byte[] dataInvalidLength = new byte[MpSpdzIntegrationUtils.SHARE_WIDTH - 1];
    SecretShare.SecretShareBuilder<?, ?> secretShareBuilder = SecretShare.builder();
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> secretShareBuilder.data(dataInvalidLength));
    assertEquals(
        String.format(INVALID_LENGTH_EXCEPTION_MSG, MpSpdzIntegrationUtils.SHARE_WIDTH),
        iae.getMessage());
  }

  @Test
  void givenTwoSecretShareWithSameContent_whenCompareEqual_thenMatch() {
    byte[] data = new byte[MpSpdzIntegrationUtils.SHARE_WIDTH];
    random.nextBytes(data);
    SecretShare secretShare1 =
        SecretShare.builder().secretId(testSecretId).data(data).tags(new ArrayList<>()).build();
    SecretShare secretShare2 =
        SecretShare.builder().secretId(testSecretId).data(data).tags(new ArrayList<>()).build();
    assertEquals(secretShare1, secretShare2);
  }

  @Test
  void givenDataOfVariableButValidLength_whenBuildSecretShare_thenSucceed() {
    for (int i = 0; i < 100; i++) {
      byte[] expectedData = new byte[MpSpdzIntegrationUtils.SHARE_WIDTH * i];
      random.nextBytes(expectedData);
      assertEquals(
          expectedData,
          SecretShare.builder()
              .secretId(testSecretId)
              .data(expectedData)
              .tags(new ArrayList<>())
              .build()
              .getData());
    }
  }

  @Test
  void givenSecretShareObject_whenSerialize_thenReturnExpectedJsonString()
      throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    byte[] data = new byte[MpSpdzIntegrationUtils.SHARE_WIDTH];
    random.nextBytes(data);
    List<Tag> tags =
        Collections.singletonList(Tag.builder().key("theKey").value("someValue").build());
    SecretShare share = SecretShare.builder().secretId(testSecretId).data(data).tags(tags).build();

    assertThat(
        om.writeValueAsString(share),
        CoreMatchers.equalTo(
            String.format(
                "{\"secretId\":\"%s\",\"tags\":%s,\"data\":%s}",
                testSecretId, om.writeValueAsString(tags), om.writeValueAsString(data))));
  }

  @Test
  void givenValidSecretShareJsonString_whenDeserialize_thenReturnExpectedObject()
      throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    byte[] data = new byte[MpSpdzIntegrationUtils.SHARE_WIDTH];
    random.nextBytes(data);
    List<Tag> tags =
        Collections.singletonList(Tag.builder().key("theKey").value("someValue").build());
    SecretShare expectedShare =
        SecretShare.builder().secretId(testSecretId).data(data).tags(tags).build();

    SecretShare actualShare =
        om.readerFor(SecretShare.class)
            .readValue(
                String.format(
                    "{\"secretId\":\"%s\",\"tags\":%s,\"data\":%s}",
                    testSecretId, om.writeValueAsString(tags), om.writeValueAsString(data)));
    assertThat(actualShare, CoreMatchers.equalTo(expectedShare));
  }
}
