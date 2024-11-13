/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Objects;

@Setter()
@Accessors(chain = true, fluent = true)
public class OpaClientRequest {
    private String withPolicyPackage;
    private String withSubject;
    private List<Tag> withTags;
    private String withAction;

    private final OpaClient opaClient;

    public OpaClientRequest(OpaClient opaClient, String defaultPolicyPackage) {
        this.withPolicyPackage = defaultPolicyPackage;
        this.opaClient = opaClient;
    }

    public boolean evaluate() throws CsOpaException {
        if(Strings.isEmpty(withSubject)) {
            throw new CsOpaException("Subject is required to evaluate the policy");
        }
        if(Strings.isEmpty(withAction)) {
            throw new CsOpaException("Action is required to evaluate the policy");
        }
        withTags.removeIf(Objects::isNull);
        return opaClient.isAllowed(withPolicyPackage, withAction, withSubject, withTags);
    }
}

