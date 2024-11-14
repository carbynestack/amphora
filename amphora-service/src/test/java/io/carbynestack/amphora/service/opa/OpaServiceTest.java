/*
 * Copyright (c) 2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.opa;

import com.google.common.collect.Lists;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.carbynestack.amphora.service.opa.OpaService.READ_SECRET_ACTION_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OpaServiceTest {

    private static final String POLICY_PACKAGE = "play";
    private static final String DEFAULT_POLICY_PACKAGE = "default";
    private static final String SUBJECT = "me";
    private static final List<Tag> TAGS = Lists.newArrayList(
            Tag.builder().key("created").value("yesterday").build(),
            Tag.builder().key("owner").value("me").build()
    );
    private static final Tag POLICY_TAG = Tag.builder()
            .key(OpaService.POLICY_PACKAGE_TAG_KEY)
            .value(POLICY_PACKAGE).build();
    private static final OpaResult POSITIVE_RESULT;
    static {
        POSITIVE_RESULT = new OpaResult();
        POSITIVE_RESULT.setResult(true);
    }

    @Mock
    private OpaClient opaClientMock = mock(OpaClient.class);

    @BeforeEach
    public void setUp() {
        reset(opaClientMock);
        when(opaClientMock.newRequest()).thenReturn(new OpaClientRequest(opaClientMock, DEFAULT_POLICY_PACKAGE));
    }

    @Test
    public void givenPolicyDefinedInTag_whenIsAllowed_thenUsePolicyPackageProvided() throws CsOpaException {
        ArgumentCaptor<String> packageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Tag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        when(opaClientMock.isAllowed(
                packageCaptor.capture(), actionCaptor.capture(), subjectCaptor.capture(), tagsCaptor.capture()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        List<Tag> testTags = Lists.newArrayList(TAGS);
        testTags.add(POLICY_TAG);
        boolean result = service.isAllowed(SUBJECT, READ_SECRET_ACTION_NAME, testTags);

        assertTrue("must be allowed", result);
        String actualPackage = packageCaptor.getValue();
        assertEquals(POLICY_TAG.getValue(), actualPackage);
        String actualAction = actionCaptor.getValue();
        assertEquals(READ_SECRET_ACTION_NAME, actualAction);
        String actualSubject = subjectCaptor.getValue();
        assertEquals(SUBJECT, actualSubject);
        List<Tag> actualTags = tagsCaptor.getValue();
        assertEquals(testTags, actualTags);
    }

    @Test
    public void givenNoPolicyDefinedInTag_whenIsAllowed_thenUseDefaultPolicyPackage() throws CsOpaException {
        ArgumentCaptor<String> packageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<Tag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
        when(opaClientMock.isAllowed(
                packageCaptor.capture(), any(), any(), tagsCaptor.capture()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        boolean result = service.isAllowed(READ_SECRET_ACTION_NAME, SUBJECT, TAGS);

        assertTrue("must be allowed", result);
        String actualPackage = packageCaptor.getValue();
        assertEquals(DEFAULT_POLICY_PACKAGE, actualPackage);
        List<Tag> actualTags = tagsCaptor.getValue();
        assertEquals(TAGS, actualTags);
    }

    @Test
    public void whenCanReadSecret_thenUseProperAction() throws CsOpaException {
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        when(opaClientMock.isAllowed(
                any(), actionCaptor.capture(), any(), any()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        service.canReadSecret(SUBJECT, TAGS);

        String actualAction = actionCaptor.getValue();
        assertEquals(READ_SECRET_ACTION_NAME, actualAction);

    }

    @Test
    public void whenCanDeleteSecret_thenUseProperAction() throws CsOpaException {
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        when(opaClientMock.isAllowed(
                any(), actionCaptor.capture(), any(), any()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        service.canDeleteSecret(SUBJECT, TAGS);

        String actualAction = actionCaptor.getValue();
        assertEquals(OpaService.DELETE_SECRET_ACTION_NAME, actualAction);
    }

    @Test
    public void whenCanCreateTags_thenUseProperAction() throws CsOpaException {
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        when(opaClientMock.isAllowed(
                any(), actionCaptor.capture(), any(), any()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        service.canCreateTags(SUBJECT, TAGS);

        String actualAction = actionCaptor.getValue();
        assertEquals(OpaService.CREATE_TAG_ACTION_NAME, actualAction);
    }

    @Test
    public void whenCanReadTags_thenUseProperAction() throws CsOpaException {
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        when(opaClientMock.isAllowed(
                any(), actionCaptor.capture(), any(), any()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        service.canReadTags(SUBJECT, TAGS);

        String actualAction = actionCaptor.getValue();
        assertEquals(OpaService.READ_TAG_ACTION_NAME, actualAction);
    }

    @Test
    public void whenCanUpdateTags_thenUseProperAction() throws CsOpaException {
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        when(opaClientMock.isAllowed(
                any(), actionCaptor.capture(), any(), any()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        service.canUpdateTags(SUBJECT, TAGS);

        String actualAction = actionCaptor.getValue();
        assertEquals(OpaService.UPDATE_TAG_ACTION_NAME, actualAction);
    }

    @Test
    public void whenCanDeleteTags_thenUseProperAction() throws CsOpaException {
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        when(opaClientMock.isAllowed(
                any(), actionCaptor.capture(), any(), any()))
                .thenReturn(true);

        OpaService service = new OpaService(opaClientMock);
        service.canDeleteTags(SUBJECT, TAGS);

        String actualAction = actionCaptor.getValue();
        assertEquals(OpaService.DELETE_TAG_ACTION_NAME, actualAction);
    }
}