package io.carbynestack.amphora.service.calculation;

import io.carbynestack.castor.common.entities.Share;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;

import java.math.BigInteger;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;

public class SemiAdditiveSecretShareConverter extends SecretShareConverter {

  private final boolean useZeroAsData;
  private final MpSpdzIntegrationUtils spdzUtil;
  private final BigInteger zeroInput;

  public SemiAdditiveSecretShareConverter(boolean useZeroAsData, MpSpdzIntegrationUtils spdzUtil) {
    this.useZeroAsData = useZeroAsData;
    this.spdzUtil = spdzUtil;
    this.zeroInput = spdzUtil.fromGfp(new byte[MpSpdzIntegrationUtils.WORD_WIDTH]);
  }

  @Override
  protected byte[] computeSecretShare(byte[] maskedInput, Share inputMask) {
    int shareSize = INPUT_MASK_GFP.getField().getElementSize();
    byte[] share = new byte[shareSize];
    BigInteger individualMaskInput = this.useZeroAsData ? zeroInput : spdzUtil.fromGfp(maskedInput);
    BigInteger shareValue = spdzUtil.fromGfp(inputMask.getValue());

    System.arraycopy(
        spdzUtil.toGfp(shareValue.add(individualMaskInput).mod(spdzUtil.getPrime())),
        0,
        share,
        0,
        shareSize);

    return share;
  }
}
