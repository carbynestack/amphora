/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.util;

import static org.mockito.Mockito.*;

import java.net.URI;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

public class ServletUriComponentsBuilderUtil {
  public static void runInMockedHttpRequestContextForUri(URI uri, Runnable runnable) {
    try (MockedStatic<ServletUriComponentsBuilder> componentsBuilder =
        Mockito.mockStatic(ServletUriComponentsBuilder.class)) {
      ServletUriComponentsBuilder builder = mock(ServletUriComponentsBuilder.class, RETURNS_SELF);
      when(builder.build()).thenReturn(UriComponentsBuilder.fromHttpUrl(uri.toString()).build());
      componentsBuilder
          .when(ServletUriComponentsBuilder::fromCurrentRequestUri)
          .thenReturn(builder);
      runnable.run();
    }
  }
}
