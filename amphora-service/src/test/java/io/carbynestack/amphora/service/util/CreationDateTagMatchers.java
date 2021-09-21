/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.util;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

@RequiredArgsConstructor
public class CreationDateTagMatchers extends TypeSafeMatcher<List<Tag>> {
  private final long start, end;
  private String failureMsg = "";

  @Override
  protected boolean matchesSafely(List<Tag> tags) {
    Optional<Tag> createdTag =
        tags.stream().filter(t -> t.getKey().equals(StorageService.CREATION_DATE_KEY)).findFirst();
    if (!createdTag.isPresent()) {
      failureMsg = String.format("Tag <%s> must be given.", StorageService.CREATION_DATE_KEY);
      return false;
    }
    long timestamp = Long.parseLong(createdTag.get().getValue());
    if (timestamp > end || timestamp < start) {
      failureMsg =
          String.format(
              "%s was supposed to be within %d and %d but was <%d>",
              StorageService.CREATION_DATE_KEY, start, end, timestamp);
      return false;
    }
    return true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(
        String.format(
            "contains Tag <%s> with value between %d and %d",
            StorageService.CREATION_DATE_KEY, start, end));
  }

  @Override
  public void describeMismatchSafely(List<Tag> tags, Description mismatchDescription) {
    mismatchDescription.appendText(failureMsg);
  }

  public static Matcher<List<Tag>> containsCreationDateTagInRange(long from, long to) {
    return new CreationDateTagMatchers(from, to);
  }

  public static Matcher<List<Tag>> containsCreationDateTag() {
    return new CreationDateTagMatchers(Long.MIN_VALUE, Long.MAX_VALUE);
  }
}
