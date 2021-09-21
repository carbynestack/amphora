/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.config;

import io.carbynestack.amphora.client.DefaultAmphoraInterVcpClient;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {CastorClientProperties.class})
public class InterVcpConfig {

  @Bean
  DefaultAmphoraInterVcpClient interVcpClient(AmphoraServiceProperties serviceProperties)
      throws AmphoraClientException {
    return DefaultAmphoraInterVcpClient.Builder()
        .withServiceUris(serviceProperties.getVcPartners())
        .withoutSslValidation(serviceProperties.isNoSslValidation())
        .withTrustedCertificates(serviceProperties.getTrustedCertificates())
        .build();
  }
}
