package io.carbynestack.amphora.service.calculation;

import io.carbynestack.amphora.common.MaskedInput;
import io.carbynestack.amphora.common.MaskedInputData;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.ShareFamily;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.Share;
import io.carbynestack.castor.common.entities.TupleList;

import java.nio.ByteBuffer;
import java.util.List;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static org.springframework.util.Assert.isTrue;

public abstract class SecretShareConverter {

  /**
   * Converts a given {@link MaskedInput} to a MPC party indivual {@link SecretShare}
   *
   * @param maskedInput the broadcasted {@link MaskedInput} to be converted
   * @param inputMask the shares of this mpc party's (this <i>Amphora</i> service) shares of the
   *     {@link InputMask}s used to generate the {@link MaskedInput}
   * @return the generated {@link SecretShare}
   */
  public SecretShare convert(
      MaskedInput maskedInput,
      TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMask,
      ShareFamily shareFamily) {
    List<MaskedInputData> maskedInputData = maskedInput.getData();
    isTrue(
        maskedInputData.size() == inputMask.size(),
        "Received more input data than available inputMasks.");
    ByteBuffer bb =
        ByteBuffer.allocate(
            maskedInputData.size() * INPUT_MASK_GFP.getTupleSize(shareFamily.getFamilyName()));
    for (int i = 0; i < maskedInputData.size(); i++) {
      bb.put(computeSecretShare(maskedInputData.get(i).getValue(), inputMask.get(i).getShare(0)));
    }
    return SecretShare.builder()
        .secretId(maskedInput.getSecretId())
        .data(bb.array())
        .tags(maskedInput.getTags())
        .build();
  }

  protected abstract byte[] computeSecretShare(byte[] maskedInput, Share inputMask);
}
