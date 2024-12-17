/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import lombok.Getter;

@Getter
class OpaRequest {
  OpaRequestBody input;

  public OpaRequest(OpaRequestBody input) {
    this.input = input;
  }
}
