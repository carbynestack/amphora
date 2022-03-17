/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * {@link TagFilter} are used to define criteria based on which the <i>Amphora Service</i> can
 * decide which {@link SecretShare}s to return on a user's request.
 *
 * <p>Tag keys are always matched exactly.<br>
 * Tag values which are Strings are always matched exactly and must use the {@link
 * TagFilterOperator#EQUALS} operator.<br>
 * Tag values which are longs can be matched with {@link TagFilterOperator#EQUALS}, {@link
 * TagFilterOperator#LESS_THAN} and {@link TagFilterOperator#GREATER_THAN}.
 *
 * <p>New {@link TagFilter} are created using the static constructor {@link TagFilter#with(String,
 * String, TagFilterOperator)}.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TagFilter implements Serializable {
  public static final String ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG =
      "The given String cannot be parsed as a TagFilter: \"%s\".";
  private static final long serialVersionUID = -728049498488774915L;

  /** The key on whose value the filter is applied on */
  @NonNull String key;

  /**
   * The comparison value against which the actual tag's value is matched against according to the
   * given comparison operator.
   */
  @NonNull String value;

  /**
   * The comparison {@link TagFilterOperator operator} used to match the {@link TagFilter}'s value
   * against the {@link Tag}'s value
   */
  @NonNull TagFilterOperator operator;

  /**
   * Creates a new {@link TagFilter} with the given configuration.
   *
   * @param key the key on whose value the filter will be applied on
   * @param value the comparison value
   * @param operator the comparison operator
   * @return the new {@link TagFilter}
   * @throws IllegalArgumentException if the defined key or comparison value violates the
   *     requirements for the respective fields (see {@link Tag#getKey()} or {@link TagValueType}).
   */
  public static TagFilter with(String key, String value, TagFilterOperator operator) {
    Tag.validateKey(key);
    switch (operator) {
      case LESS_THAN:
      case GREATER_THAN:
        TagValueType.LONG.accept(value);
        break;
      case EQUALS:
        // when comparing for equality, longs can be treated as strings too. No need to
        // differentiate
        TagValueType.STRING.accept(value);
        break;
    }
    return new TagFilter(key, value, operator);
  }

  /**
   * Parses a string and creates a new {@link TagFilter} with the derived configuration.
   *
   * <p>String format is expected to match the following pattern:<br>
   *
   * <pre>    &lt;key&gt;&lt;operator&gt;&lt;value&gt;</pre>
   *
   * <p>This method accepts and decodes URL encoded strings for key and value (see {@link
   * URLDecoder}).
   *
   * @return the new {@link TagFilter}
   * @throws IllegalArgumentException if the given string cannot be parsed to a valid {@link
   *     TagFilter}.
   */
  public static TagFilter fromString(String filterString) throws UnsupportedEncodingException {
    String operatorGroup =
        "("
            + Arrays.stream(TagFilterOperator.values())
                .map(TagFilterOperator::toString)
                .collect(Collectors.joining("|"))
            + ")";
    Pattern filterPattern = Pattern.compile("^([\\w-.%]+?)" + operatorGroup + "([\\w-.%]+?)$");
    Matcher matcher = filterPattern.matcher(filterString);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          String.format(ILLEGAL_TAGFILTER_FORMAT_EXCEPTION_MSG, filterString));
    }
    return TagFilter.with(
        URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8.name()),
        URLDecoder.decode(matcher.group(3), StandardCharsets.UTF_8.name()),
        TagFilterOperator.fromString(matcher.group(2)));
  }
}
