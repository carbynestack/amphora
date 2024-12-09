/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.calculation;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.service.config.SpdzProperties;
import io.carbynestack.castor.common.entities.*;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecretShareConverterTest {
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");

  private SpdzProperties spdzProperties;
  private SecretShareConverter secretShareConverter;
  private MpSpdzIntegrationUtils spdzUtil;

  @BeforeEach
  public void prepare() {
    spdzProperties = new SpdzProperties();
    spdzProperties.setPrime(new BigInteger("198766463529478683931867765928436695041"));
    spdzProperties.setR(new BigInteger("141515903391459779531506841503331516415"));
    spdzProperties.setRInv(new BigInteger("133854242216446749056083838363708373830"));
    spdzUtil =
        MpSpdzIntegrationUtils.of(
            spdzProperties.getPrime(), spdzProperties.getR(), spdzProperties.getRInv());
    BigInteger macKey =
        new BigInteger("-33717010807885571165607137982809795379").mod(spdzProperties.getPrime());
    secretShareConverter = new AdditiveSecretShareConverter(macKey.toString(), false, spdzUtil);
  }

  @Test
  void
      givenLessInputMasksThanMaskedInput_whenConvertToSecretShare_thenThrowIllegalArgumentException() {
    List<MaskedInputData> maskedInputDataList = new ArrayList<>();
    maskedInputDataList.add(MaskedInputData.of(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]));
    TupleList inputMaskList =
        new TupleList(
            INPUT_MASK_GFP.getTupleCls(),
            ShareFamily.COWGEAR.getFamilyName(),
            INPUT_MASK_GFP.getField());
    MaskedInput maskedInput =
        new MaskedInput(
            UUID.fromString("93448822-fc76-4989-a927-450486ae0a08"),
            maskedInputDataList,
            new ArrayList<>());

    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> secretShareConverter.convert(maskedInput, inputMaskList, ShareFamily.COWGEAR));
    assertEquals("Received more input data than available inputMasks.", iae.getMessage());
  }

  @Test
  void givenCorrectArguments_whenConvertToSecretShare_thenReturnExpectedResult() {
    List<Tag> tags =
        Arrays.asList(
            Tag.builder().key("key1").value("value1").build(),
            Tag.builder().key("key2").value("value3").build());
    List<MaskedInputData> maskedInputData =
        maskedInputDataToList(
            new BigInteger("37371993412255263319479925008425883363").mod(spdzProperties.getPrime()),
            new BigInteger("0"));
    MaskedInput maskedInput = new MaskedInput(testSecretId, maskedInputData, tags);
    TupleList inputMasks =
        inputMaskToList(
            makeInputMask(
                new BigInteger("-82730997414791468496799367418496881908")
                    .mod(spdzProperties.getPrime()),
                new BigInteger("-60557275363670854182192939229091375859")
                    .mod(spdzProperties.getPrime())),
            makeInputMask(
                new BigInteger("45359004002536205186084333850157344582")
                    .mod(spdzProperties.getPrime()),
                new BigInteger("-48604663536222227589564560476962533035")
                    .mod(spdzProperties.getPrime())));
    SecretShare expectedSecretShare =
        makeSecretShare(
            testSecretId,
            tags,
            new BigInteger("-45359004002536205177319442410070998545")
                .mod(spdzProperties.getPrime()),
            new BigInteger("-170814686092998134911558977038957876158")
                .mod(spdzProperties.getPrime()),
            new BigInteger("45359004002536205186084333850157344582").mod(spdzProperties.getPrime()),
            new BigInteger("-48604663536222227589564560476962533035")
                .mod(spdzProperties.getPrime()));
    assertEquals(
        expectedSecretShare,
        secretShareConverter.convert(maskedInput, inputMasks, ShareFamily.COWGEAR));
  }

  private List<MaskedInputData> maskedInputDataToList(BigInteger... maskedInputs) {
    List<MaskedInputData> maskedInputList = new ArrayList<>();
    for (BigInteger bi : maskedInputs) {
      maskedInputList.add(MaskedInputData.of(spdzUtil.toGfp(bi)));
    }
    return maskedInputList;
  }

  private TupleList inputMaskToList(InputMask... inputMasks) {
    TupleList inputMaskList =
        new TupleList(
            TupleType.INPUT_MASK_GFP.getTupleCls(),
            ShareFamily.COWGEAR.getFamilyName(),
            TupleType.INPUT_MASK_GFP.getField());
    inputMaskList.addAll(Arrays.asList(inputMasks));
    return inputMaskList;
  }

  private InputMask makeInputMask(BigInteger value, BigInteger key) {
    Share share = new Share(spdzUtil.toGfp(value), spdzUtil.toGfp(key));
    InputMask inputMask = new InputMask(Field.GFP, share);
    return inputMask;
  }

  private SecretShare makeSecretShare(UUID secretId, List<Tag> tags, BigInteger... words) {
    ByteBuffer bb = ByteBuffer.allocate(words.length * MpSpdzIntegrationUtils.WORD_WIDTH);
    for (BigInteger word : words) {
      bb.put(spdzUtil.toGfp(word));
    }
    return SecretShare.builder().secretId(secretId).data(bb.array()).tags(tags).build();
  }
}
