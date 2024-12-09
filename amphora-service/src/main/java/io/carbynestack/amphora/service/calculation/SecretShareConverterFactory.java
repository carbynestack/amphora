package io.carbynestack.amphora.service.calculation;

import io.carbynestack.amphora.common.ShareFamily;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.config.AmphoraServiceProperties;
import io.carbynestack.amphora.service.config.SpdzProperties;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SecretShareConverterFactory {

  private final MpSpdzIntegrationUtils spdzUtil;
  private final SpdzProperties spdzProperties;
  private final AmphoraServiceProperties amphoraServiceProperties;

  public SecretShareConverter createShareConverter(Optional<Tag> shareFamily) {
    return shareFamily
        .map(tag -> createByFamily(ShareFamily.valueOf(tag.getValue().toUpperCase())))
        .orElseGet(
            () ->
                new AdditiveSecretShareConverter(
                    spdzProperties.getMacKey(),
                    amphoraServiceProperties.getPlayerId() != 0,
                    spdzUtil));
  }

  private SecretShareConverter createByFamily(ShareFamily shareFamily) {
    switch (shareFamily) {
      case HEMI:
        return new SemiAdditiveSecretShareConverter(
            amphoraServiceProperties.getPlayerId() != 0, spdzUtil);
      case COWGEAR:
      default:
        return new AdditiveSecretShareConverter(
            spdzProperties.getMacKey(), amphoraServiceProperties.getPlayerId() != 0, spdzUtil);
    }
  }
}
