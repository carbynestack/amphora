/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.config;

import java.math.BigInteger;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "carbynestack.spdz")
@Component
@Data
@Accessors(chain = true)
public class SpdzProperties {

  private String macKey;

  private BigInteger prime;
  private BigInteger r;
  private BigInteger rInv;
}
