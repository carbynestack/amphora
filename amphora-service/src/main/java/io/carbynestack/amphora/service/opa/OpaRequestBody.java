/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import io.carbynestack.amphora.common.Tag;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
public class OpaRequestBody {
    String subject;
    long currentTime;
    List<Tag> tags;

    @Builder
    public OpaRequestBody(String subject, List<Tag> tags) {
        this.subject = subject;
        this.currentTime = System.currentTimeMillis();
        this.tags = tags;
    }

    OpaRequestBody(String subject, long currentTime, List<Tag> tags) {
        this.subject = subject;
        this.currentTime = currentTime;
        this.tags = tags;
    }
}
