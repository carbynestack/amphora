/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.config;

import io.carbynestack.amphora.service.opa.OpaClient;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.client.download.DefaultCastorIntraVcpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;

@Configuration
public class OpaConfig {

  @Bean
  OpaClient opaClient(OpaProperties opaProperties) {
    return OpaClient.builder()
            .opaServiceUri(URI.create(opaProperties.getEndpoint()))
            .defaultPolicyPackage(opaProperties.getDefaultPolicyPackage())
            .build();
  }
}
