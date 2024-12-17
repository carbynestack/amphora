/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import static io.carbynestack.amphora.service.opa.OpaService.READ_SECRET_ACTION_NAME;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import io.carbynestack.httpclient.CsHttpClient;
import io.carbynestack.httpclient.CsHttpClientException;
import java.net.URI;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpaClientTest {

  private static final URI OPA_SERVICE_URI = URI.create("http://localhost:8081");
  private static final String POLICY_PACKAGE = "play";
  private static final String DEFAULT_POLICY_PACKAGE = "default";
  private static final String SUBJECT = "me";
  private static final List<Tag> TAGS =
      Lists.newArrayList(
          Tag.builder().key("created").value("yesterday").build(),
          Tag.builder().key("owner").value("me").build());
  private static final OpaResult POSITIVE_RESULT;

  static {
    POSITIVE_RESULT = new OpaResult();
    POSITIVE_RESULT.setResult(true);
  }

  private static final OpaResult NEGATIVE_RESULT = new OpaResult();

  @Mock private CsHttpClient<String> csHttpClientMock = mock(CsHttpClient.class);

  @BeforeEach
  public void setUp() {
    reset(csHttpClientMock);
  }

  @Test
  void givenValidRequest_whenEvaluate_thenReturnTrue()
      throws CsOpaException, CsHttpClientException {
    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    ArgumentCaptor<OpaRequest> requestCaptor = ArgumentCaptor.forClass(OpaRequest.class);
    when(csHttpClientMock.postForObject(
            uriCaptor.capture(), requestCaptor.capture(), eq(OpaResult.class)))
        .thenReturn(NEGATIVE_RESULT);

    OpaClient opaClient = new OpaClient(csHttpClientMock, OPA_SERVICE_URI, DEFAULT_POLICY_PACKAGE);
    opaClient
        .newRequest()
        .withPolicyPackage(POLICY_PACKAGE)
        .withAction(READ_SECRET_ACTION_NAME)
        .withSubject(SUBJECT)
        .withTags(TAGS)
        .evaluate();

    URI actualUri = uriCaptor.getValue();
    MatcherAssert.assertThat(actualUri.toString(), Matchers.startsWith(OPA_SERVICE_URI.toString()));
    MatcherAssert.assertThat(
        actualUri.toString(),
        Matchers.endsWith(
            String.format("/v1/data/%s/%s", POLICY_PACKAGE, READ_SECRET_ACTION_NAME)));
    OpaRequestBody actualRequestBody = requestCaptor.getValue().getInput();
    assertEquals(SUBJECT, actualRequestBody.getSubject());
    assertEquals(TAGS, actualRequestBody.getTags());
  }

  @Test
  void givenNoPolicyPackageDefined_whenEvaluate_thenReturnUseDefaultPackage()
      throws CsOpaException, CsHttpClientException {
    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    ArgumentCaptor<OpaRequest> requestCaptor = ArgumentCaptor.forClass(OpaRequest.class);
    when(csHttpClientMock.postForObject(
            uriCaptor.capture(), requestCaptor.capture(), eq(OpaResult.class)))
        .thenReturn(NEGATIVE_RESULT);

    OpaClient opaClient = new OpaClient(csHttpClientMock, OPA_SERVICE_URI, DEFAULT_POLICY_PACKAGE);
    opaClient
        .newRequest()
        .withAction(READ_SECRET_ACTION_NAME)
        .withSubject(SUBJECT)
        .withTags(TAGS)
        .evaluate();

    URI actualUri = uriCaptor.getValue();
    MatcherAssert.assertThat(actualUri.toString(), Matchers.startsWith(OPA_SERVICE_URI.toString()));
    MatcherAssert.assertThat(
        actualUri.toString(),
        Matchers.endsWith(
            String.format("/v1/data/%s/%s", DEFAULT_POLICY_PACKAGE, READ_SECRET_ACTION_NAME)));
    OpaRequestBody actualRequestBody = requestCaptor.getValue().getInput();
    assertEquals(SUBJECT, actualRequestBody.getSubject());
    assertEquals(TAGS, actualRequestBody.getTags());
  }

  @Test
  void givenOpaReturnsFalse_whenEvaluate_thenReturnFalse()
      throws CsHttpClientException, CsOpaException {
    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    ArgumentCaptor<OpaRequest> requestCaptor = ArgumentCaptor.forClass(OpaRequest.class);
    when(csHttpClientMock.postForObject(
            uriCaptor.capture(), requestCaptor.capture(), eq(OpaResult.class)))
        .thenReturn(NEGATIVE_RESULT);

    OpaClient opaClient = new OpaClient(csHttpClientMock, OPA_SERVICE_URI, DEFAULT_POLICY_PACKAGE);
    boolean result =
        opaClient
            .newRequest()
            .withAction(READ_SECRET_ACTION_NAME)
            .withSubject(SUBJECT)
            .withTags(TAGS)
            .evaluate();

    assertFalse(result, "must not be allowed");
  }

  @Test
  void givenNoSubjectDefined_whenEvaluate_thenThrowException() {
    OpaClient opaClient = new OpaClient(csHttpClientMock, OPA_SERVICE_URI, DEFAULT_POLICY_PACKAGE);
    try {
      opaClient.newRequest().withAction(READ_SECRET_ACTION_NAME).withTags(TAGS).evaluate();
      fail("must throw exception");
    } catch (CsOpaException e) {
      assertEquals("Subject is required to evaluate the policy", e.getMessage());
    }
  }

  @Test
  void givenNoActionDefined_whenEvaluate_thenThrowException() {
    OpaClient opaClient = new OpaClient(csHttpClientMock, OPA_SERVICE_URI, DEFAULT_POLICY_PACKAGE);
    try {
      opaClient.newRequest().withSubject(SUBJECT).withTags(TAGS).evaluate();
      fail("must throw exception");
    } catch (CsOpaException e) {
      assertEquals("Action is required to evaluate the policy", e.getMessage());
    }
  }

  @Test
  void givenClientThrows_whenEvaluate_thenReturnFalse()
      throws CsHttpClientException, CsOpaException {
    when(csHttpClientMock.postForObject(any(), any(), eq(OpaResult.class)))
        .thenThrow(new CsHttpClientException(""));

    OpaClient opaClient = new OpaClient(csHttpClientMock, OPA_SERVICE_URI, DEFAULT_POLICY_PACKAGE);
    boolean result =
        opaClient
            .newRequest()
            .withAction(READ_SECRET_ACTION_NAME)
            .withSubject(SUBJECT)
            .withTags(TAGS)
            .evaluate();
    assertFalse(result, "must not be allowed");
  }
}
