/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.util;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MetadataMatchers extends TypeSafeMatcher<Metadata> {
  private final Metadata origin;
  private final boolean ignoreReservedTags;

  @Override
  protected boolean matchesSafely(Metadata item) {
    List<Tag> tagsToContain =
        ignoreReservedTags
            ? item.getTags().stream()
                .filter(t -> !StorageService.tagIsReserved(t))
                .collect(Collectors.toList())
            : item.getTags();
    return Matchers.equalTo(origin.getSecretId()).matches(item.getSecretId())
        && Matchers.containsInAnyOrder(tagsToContain.toArray()).matches(origin.getTags());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(
        String.format(
            "equal to <%s> %s",
            origin.toString(),
            ignoreReservedTags
                ? String.format("but ignore Tag <%s>", StorageService.CREATION_DATE_KEY)
                : ""));
  }

  public static Matcher<Metadata> equalsButIgnoreReservedTags(Metadata origin) {
    return new MetadataMatchers(origin, true);
  }
}
