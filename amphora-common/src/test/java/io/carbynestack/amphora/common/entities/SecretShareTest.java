/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.entities;

import static io.carbynestack.amphora.common.SecretShare.INVALID_LENGTH_EXCEPTION_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.util.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class SecretShareTest {
  final Random random = new Random(42);
  final UUID testSecretId = UUID.fromString("80fbba1b-3da8-4b1e-8a2c-cebd65229fad");

  @Test
  void givenTwoSecretSharesWithSameContent_whenCompareEqual_thenMatch() {
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

  @SneakyThrows
  @Test
  void givenSecretShareObject_whenSerialize_thenReturnExpectedJsonString() {
    ObjectMapper om = new ObjectMapper();
    byte[] data = new byte[MpSpdzIntegrationUtils.SHARE_WIDTH];
    random.nextBytes(data);
    List<Tag> tags =
        Collections.singletonList(Tag.builder().key("theKey").value("someValue").build());
    SecretShare share = SecretShare.builder().secretId(testSecretId).data(data).tags(tags).build();

    assertEquals(
        String.format(
            "{\"secretId\":\"%s\",\"tags\":%s,\"data\":%s}",
            testSecretId, om.writeValueAsString(tags), om.writeValueAsString(data)),
        om.writeValueAsString(share));
  }

  @Test
  @SneakyThrows
  public void givenValidSecretShareJsonString_whenDeserialize_thenReturnExpectedObject() {
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
    assertEquals(expectedShare, actualShare);
  }
}
