/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import lombok.Setter;

public class OpaResult {
    @Setter
    private boolean result;

    boolean isAllowed() {
        return result;
    }
}
