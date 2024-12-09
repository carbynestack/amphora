package io.carbynestack.amphora.service.calculation;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.castor.common.entities.Share;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;

import java.math.BigInteger;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;

public class AdditiveSecretShareConverter extends SecretShareConverter {

  private final String macKey;
  private final boolean useZeroAsData;
  private final BigInteger zeroInput;
  private final MpSpdzIntegrationUtils spdzUtil;

  /**
   * @param macKey the mpc party's (this <i>Amphora</i> service) mac key
   * @param useZeroAsData a flag indicating whether a so called zero input should be used to
   *     generate the individual {@link SecretShare}'s {@link SecretShare#getData() data}
   * @param spdzUtil
   */
  public AdditiveSecretShareConverter(
      String macKey, boolean useZeroAsData, MpSpdzIntegrationUtils spdzUtil) {
    this.macKey = macKey;
    this.useZeroAsData = useZeroAsData;
    this.zeroInput = spdzUtil.fromGfp(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]);
    this.spdzUtil = spdzUtil;
  }

  @Override
  protected byte[] computeSecretShare(byte[] maskedInput, Share inputMask) {
    byte[] share = new byte[INPUT_MASK_GFP.getShareSize()];
    BigInteger key = new BigInteger(macKey);
    BigInteger publicValue = spdzUtil.fromGfp(maskedInput);
    BigInteger individualMaskInput = useZeroAsData ? zeroInput : spdzUtil.fromGfp(maskedInput);
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
