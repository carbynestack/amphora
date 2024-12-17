/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.httpclient.CsHttpClient;
import io.carbynestack.httpclient.CsHttpClientException;
import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpaClient {
  private final CsHttpClient<String> csHttpClient;
  private final URI opaServiceUri;
  private final String defaultPolicyPackage;

  @Builder
  public OpaClient(URI opaServiceUri, String defaultPolicyPackage) {
    this(CsHttpClient.createDefault(), opaServiceUri, defaultPolicyPackage);
  }

  OpaClient(CsHttpClient<String> httpClient, URI opaServiceUri, String defaultPolicyPackage) {
    this.csHttpClient = httpClient;
    this.opaServiceUri = opaServiceUri;
    this.defaultPolicyPackage = defaultPolicyPackage;
  }

  /**
   * Evaluate the OPA policy package with the given action, subject and tags.
   *
   * @param policyPackage The OPA policy package to evaluate.
   * @param action The action to evaluate.
   * @param subject The subject attempting to perform the action.
   * @param tags The tags describing the accessed object.
   * @return True if the subject can perform the action, false otherwise (or if an error occurred).
   */
  public boolean isAllowed(String policyPackage, String action, String subject, List<Tag> tags) {
    OpaRequestBody body = OpaRequestBody.builder().subject(subject).tags(tags).build();
    try {
      return csHttpClient
          .postForObject(
              opaServiceUri.resolve(
                  String.format("/v1/data/%s/%s", policyPackage.replace(".", "/"), action)),
              new OpaRequest(body),
              OpaResult.class)
          .isAllowed();
    } catch (CsHttpClientException e) {
      log.error("Error occurred while evaluating OPA policy package", e);
    }
    return false;
  }

  public OpaClientRequest newRequest() {
    return new OpaClientRequest(this, defaultPolicyPackage);
  }
}
