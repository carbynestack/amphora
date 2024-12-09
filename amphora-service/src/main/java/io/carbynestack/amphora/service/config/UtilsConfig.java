/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.config;

import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class UtilsConfig {
  @Bean
  public MpSpdzIntegrationUtils spdzUtil(SpdzProperties spdzProperties) {
    return MpSpdzIntegrationUtils.of(
        spdzProperties.getPrime(), spdzProperties.getR(), spdzProperties.getRInv());
  }
}
