/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.calculation;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static org.springframework.util.Assert.isTrue;

import io.carbynestack.amphora.common.MaskedInput;
import io.carbynestack.amphora.common.MaskedInputData;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.Share;
import io.carbynestack.castor.common.entities.TupleList;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A utility class providing the basic functionality to convert a {@link MaskedInput} to an {@link
 * SecretShare}.
 */
public class SecretShareUtil {

  private final MpSpdzIntegrationUtils spdzUtil;
  private final BigInteger zeroInput;

  /**
   * Creates a new {@link SecretShareUtil}
   *
   * @param spdzUtil a {@link MpSpdzIntegrationUtils} instance initialized the cluster's SPDZ
   *     configuration
   */
  @Autowired
  public SecretShareUtil(MpSpdzIntegrationUtils spdzUtil) {
    this.spdzUtil = spdzUtil;
    zeroInput = spdzUtil.fromGfp(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]);
  }

  /**
   * Converts a given {@link MaskedInput} to a MPC party indivual {@link SecretShare}
   *
   * @param maskedInput the broadcasted {@link MaskedInput} to be converted
   * @param macKey the mpc party's (this <i>Amphora</i> service) mac key
   * @param inputMask the shares of this mpc party's (this <i>Amphora</i> service) shares of the
   *     {@link InputMask}s used to generate the {@link MaskedInput}
   * @param useZeroInputAsData a flag indicating whether a so called zero input should be used to
   *     generate the individual {@link SecretShare}'s {@link SecretShare#getData() data}
   * @return the generated {@link SecretShare}
   */
  public SecretShare convertToSecretShare(
      MaskedInput maskedInput,
      String macKey,
      TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMask,
      boolean useZeroInputAsData) {
    List<MaskedInputData> maskedInputData = maskedInput.getData();
    isTrue(
        maskedInputData.size() == inputMask.size(),
        "Received more input data than available inputMasks.");
    ByteBuffer bb = ByteBuffer.allocate(maskedInputData.size() * INPUT_MASK_GFP.getTupleSize());
    for (int i = 0; i < maskedInputData.size(); i++) {
      bb.put(
          computeSecretShare(
              macKey,
              maskedInputData.get(i).getValue(),
              inputMask.get(i).getShare(0),
              useZeroInputAsData));
    }
    return SecretShare.builder()
        .secretId(maskedInput.getSecretId())
        .data(bb.array())
        .tags(maskedInput.getTags())
        .build();
  }

  private byte[] computeSecretShare(
      String mac, byte[] maskedInput, Share inputMask, boolean useZeroInputAsData) {
    byte[] share = new byte[INPUT_MASK_GFP.getShareSize()];
    BigInteger key = new BigInteger(mac);
    BigInteger publicValue = spdzUtil.fromGfp(maskedInput);
    BigInteger individualMaskInput = useZeroInputAsData ? zeroInput : spdzUtil.fromGfp(maskedInput);
    BigInteger shareValue = spdzUtil.fromGfp(inputMask.getValue());
    BigInteger shareMac = spdzUtil.fromGfp(inputMask.getMac());

    System.arraycopy(
        spdzUtil.toGfp(shareValue.add(individualMaskInput).mod(spdzUtil.getPrime())),
        0,
        share,
        0,
        INPUT_MASK_GFP.getShareSize() / 2);

    System.arraycopy(
        spdzUtil.toGfp(shareMac.add(key.multiply(publicValue)).mod(spdzUtil.getPrime())),
        0,
        share,
        INPUT_MASK_GFP.getShareSize() / 2,
        INPUT_MASK_GFP.getShareSize() / 2);

    return share;
  }
}
