/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.config;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Set;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "carbynestack.amphora.cache")
@Component
@Data
@Accessors(chain = true)
public class AmphoraCacheProperties {

  private String inputMaskStore;
  private String interimValueStore;

  private String host;
  private int port;

  public Set<String> getCacheNames() {
    return Sets.newLinkedHashSet(Arrays.asList(inputMaskStore, interimValueStore));
  }
}
