/*
 * Copyright (c) 2021-2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils.WORD_WIDTH;

import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.ShareFamily;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.castor.common.entities.*;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomUtils;

public class AmphoraTestData {
  private static final Random random = new Random(42);

  public static TupleList<InputMask<Field.Gfp>, Field.Gfp> getRandomInputMaskList(long size) {
    TupleList inputMaskList =
        new TupleList(
            TupleType.INPUT_MASK_GFP.getTupleCls(),
            ShareFamily.COWGEAR.getFamilyName(),
            TupleType.INPUT_MASK_GFP.getField());
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

  public static OutputDeliveryObject getRandomOutputDeliveryObject(int size) {
    byte[] shareData = new byte[size * WORD_WIDTH];
    byte[] rData = new byte[size * WORD_WIDTH];
    byte[] vData = new byte[size * WORD_WIDTH];
    byte[] wData = new byte[size * WORD_WIDTH];
    byte[] uData = new byte[size * WORD_WIDTH];
    random.nextBytes(shareData);
    random.nextBytes(rData);
    random.nextBytes(vData);
    random.nextBytes(wData);
    random.nextBytes(uData);
    return new OutputDeliveryObject(shareData, rData, vData, wData, uData);
  }

  public static byte[] extractTupleValuesFromInputMaskList(
      TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasks) {
    byte[] tupleData = new byte[inputMasks.size() * Field.GFP.getElementSize()];
    IntStream.range(0, inputMasks.size())
        .parallel()
        .forEach(
            i ->
                System.arraycopy(
                    inputMasks.get(i).getShare(0).getValue(),
                    0,
                    tupleData,
                    i * Field.GFP.getElementSize(),
                    Field.GFP.getElementSize()));
    return tupleData;
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
