/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.config;

import io.carbynestack.amphora.common.AmphoraServiceUri;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "carbynestack.amphora")
@Component
@Data
@Accessors(chain = true)
public class AmphoraServiceProperties {
  private int playerId;
  private List<AmphoraServiceUri> vcPartners = new ArrayList<>();
  private boolean noSslValidation = false;
  private List<File> trustedCertificates = new ArrayList<>();
  private int openingTimeout;
}
