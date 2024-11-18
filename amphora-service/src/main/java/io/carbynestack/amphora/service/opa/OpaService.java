/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpaService {
    public static final String POLICY_PACKAGE_TAG_KEY = "accessPolicy";
    public static final String OWNER_TAG_KEY = "owner";

    static final String READ_SECRET_ACTION_NAME = "read";
    static final String USE_SECRET_ACTION_NAME = "use";
    static final String DELETE_SECRET_ACTION_NAME = "delete";
    static final String CREATE_TAG_ACTION_NAME = "tag/create";
    static final String READ_TAG_ACTION_NAME = "tag/read";
    static final String UPDATE_TAG_ACTION_NAME = "tag/update";
    static final String DELETE_TAG_ACTION_NAME = "tag/delete";

    private final OpaClient opaClient;

    @Autowired
    public OpaService(OpaClient opaClient) {
        this.opaClient = opaClient;
    }

    /**
     * Check if the subject can read the secret described by the given tags evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param subject The subject attempting to read the secret.
     * @param tags The tags describing the referenced secret.
     * @return True if the subject can read the secret, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    public boolean canReadSecret(String subject, List<Tag> tags) throws CsOpaException {
        return isAllowed(subject, READ_SECRET_ACTION_NAME, tags);
    }

    /**
     * Check if the subject can use the secret described by the given tags evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param subject The subject attempting to use the secret.
     * @param tags The tags describing the referenced secret.
     * @return True if the subject can use the secret, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    public boolean canUseSecret(String subject, List<Tag> tags) throws CsOpaException {
        return isAllowed(subject, USE_SECRET_ACTION_NAME, tags);
    }

    /**
     * Check if the subject can delete the secret described by the given tags evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param subject The subject attempting to delete the secret.
     * @param tags The tags describing the referenced secret.
     * @return True if the subject can delete the secret, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    public boolean canDeleteSecret(String subject, List<Tag> tags) throws CsOpaException {
        return isAllowed(subject, DELETE_SECRET_ACTION_NAME, tags);
    }

    /**
     * Check if the subject can create the given tags evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param subject The subject attempting to create the tags.
     * @param tags The tags to create.
     * @return True if the subject can create the tags, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    public boolean canCreateTags(String subject, List<Tag> tags) throws CsOpaException {
        return isAllowed(subject, CREATE_TAG_ACTION_NAME, tags);
    }

    /**
     * Check if the subject can read the given tags evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param subject The subject attempting to read the tags.
     * @param tags The tags to read.
     * @return True if the subject can read the tags, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    public boolean canReadTags(String subject, List<Tag> tags) throws CsOpaException {
        return isAllowed(subject, READ_TAG_ACTION_NAME, tags);
    }

    /**
     * Check if the subject can update the given tags evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param subject The subject attempting to update the tags.
     * @param tags The tags to update.
     * @return True if the subject can update the tags, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    public boolean canUpdateTags(String subject, List<Tag> tags) throws CsOpaException {
        return isAllowed(subject, UPDATE_TAG_ACTION_NAME, tags);
    }

    /**
     * Check if the subject can delete the given tags evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param subject The subject attempting to delete the tags.
     * @param tags The tags to delete.
     * @return True if the subject can delete the tags, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    public boolean canDeleteTags(String subject, List<Tag> tags) throws CsOpaException {
        return isAllowed(subject, DELETE_TAG_ACTION_NAME, tags);
    }

    /**
     * Check if the subject can perform the given action evaluating the OPA policy package.
     * The policy package is extracted from the tags if present. If not present, the default policy package is used.
     *
     * @param action The action to evaluate.
     * @param subject The subject attempting to perform the action.
     * @param tags The tags describing the accessed object.
     * @return True if the subject can perform the action, false otherwise.
     * @throws CsOpaException If an error occurred while evaluating the policy.
     */
    boolean isAllowed(String subject, String action, List<Tag> tags) throws CsOpaException {
        OpaClientRequest request = opaClient.newRequest()
                .withSubject(subject)
                .withAction(action)
                .withTags(tags);
        tags.stream().filter(tag -> tag.getKey().equals(POLICY_PACKAGE_TAG_KEY))
                .findFirst()
                .ifPresent(tag -> request.withPolicyPackage(tag.getValue()));
        return request.evaluate();
    }
}
