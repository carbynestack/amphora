/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.castor.common.entities.*;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;

public class AmphoraTestData {
  private static final Random random = new Random(42);

  public static TupleList<InputMask<Field.Gfp>, Field.Gfp> getRandomInputMaskList(long size) {
    TupleList inputMaskList =
        new TupleList(TupleType.INPUT_MASK_GFP.getTupleCls(), TupleType.INPUT_MASK_GFP.getField());
    for (int i = 0; i < size; i++) {
      Share share =
          new Share(
              RandomUtils.nextBytes(INPUT_MASK_GFP.getShareSize() / 2),
              RandomUtils.nextBytes(INPUT_MASK_GFP.getShareSize() / 2));
      InputMask inputMask = new InputMask(Field.GFP, share);
      inputMaskList.add(inputMask);
    }
    return inputMaskList;
  }

  public static SecretShare getRandomSecretShare(UUID secretShareId) {
    byte[] data = new byte[(random.nextInt(99) + 1) * MpSpdzIntegrationUtils.SHARE_WIDTH];
    random.nextBytes(data);
    return SecretShare.builder().secretId(secretShareId).data(data).tags(getTags()).build();
  }

  public static List<Tag> getTags() {
    List<Tag> tags = new ArrayList<>();
    tags.add(Tag.builder().key("TEST_KEY").value("TEST_VALUE").build());
    return tags;
  }
}
