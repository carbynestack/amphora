/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.config;

import io.carbynestack.amphora.service.opa.JwtReader;
import io.carbynestack.amphora.service.opa.OpaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class OpaConfig {

  @Bean
  JwtReader jwtReader(AuthProperties authProperties) {
    return new JwtReader(authProperties.getUserIdFieldName());
  }

  @Bean
  OpaClient opaClient(OpaProperties opaProperties) {
    return OpaClient.builder()
            .opaServiceUri(URI.create(opaProperties.getEndpoint()))
            .defaultPolicyPackage(opaProperties.getDefaultPolicyPackage())
            .build();
  }
}
