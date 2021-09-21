/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

/**
 * An entity class representing a tag for an {@link SecretShare}.
 *
 * <p>A {@link Tag} is a key-value pair used to describe characteristics of an {@link SecretShare}.
 * The {@link Tag#key} is of type {@link String} and must have a minimum length of 1 character and a
 * maximum length of {@value Tag#MAX_KEY_LENGTH} characters containing only alphanumeric characters,
 * periods (.), hyphens (-) and underscores (_).<br>
 * The {@link Tag#value} is a String of a maximum length of {@value TagValueType#MAX_VALUE_LENGTH}
 * characters containing any character. It may be empty if the {@link Tag} is to be used as a flag,
 * but must not be null.
 *
 * <p>New {@link Tag}s are created using the {@link Tag#builder() builder}.
 */
@Getter
@EqualsAndHashCode
@ToString
@Jacksonized
@Builder
public final class Tag implements Serializable {
  private static final long serialVersionUID = -294201291960791168L;
  /** Maximum allowed length for the {@link Tag#key} */
  static final int MAX_KEY_LENGTH = 128;

  public static final String INVALID_KEY_STRING_EXCEPTION_MSG =
      "A tag's key must be an alphanumeric string with a minimum length of 1. It may contain"
          + " periods (.), hyphens (-) and underscores (_).";
  public static final String INVALID_KEY_LENGTH_EXCEPTION_MSG =
      "The length of a tag's key cannot be longer then 128 characters.";
  /**
   * The key of this {@link Tag}.<br>
   * A key must meet the following requirements:
   *
   * <ul>
   *   <li>must not be <i>null</i>
   *   <li>must contain at least one character
   *   <li>must have a maximum length of {@value Tag#MAX_KEY_LENGTH}
   *   <li>must not contain any other character than
   *       <ul>
   *         <li style="padding-left:5px">alphanumeric characters (a-zA-Z0-9)
   *         <li style="padding-left:5px">periods (.)
   *         <li style="padding-left:5px">hyphens (-)
   *         <li style="padding-left:5px">underscores (_)
   *       </ul>
   * </ul>
   */
  @NonNull private String key;
  /**
   * The value of this {@link Tag}.<br>
   *
   * <p>The <b><i>default</i></b> value ist an empty {@link String} ("")
   */
  @NonNull @Builder.Default private String value = "";
  /**
   * The content type of the value.
   *
   * <p>When creating a new Tag, its value is validated to be parsable as the given {@link
   * TagValueType}.
   *
   * <p>It must not be <i>null</i> and the <b><i>default</i></b> value ist {@link
   * TagValueType#STRING}
   */
  @NonNull @Builder.Default private TagValueType valueType = TagValueType.STRING;

  /**
   * Creates a new {@link Tag}.
   *
   * <p>The given parameters are validated to match the individual requirements.
   *
   * <p>This constructor is used by <i>lombok</i> to provide an auto-generated {@link Tag#builder()
   * builder}.
   *
   * @param key the {@link Tag#key} of this {@link Tag}
   * @param value the {@link Tag#value} of this {@link Tag}
   * @param valueType the {@link Tag#valueType} of this {@link Tag}
   * @throws IllegalArgumentException If the the given parameters do not meet the individual
   *     requirements
   */
  private Tag(String key, String value, TagValueType valueType) {
    validateKey(key);
    valueType.accept(value);
    this.key = key;
    this.value = value;
    this.valueType = valueType;
  }

  /**
   * Checks if the given key meets all the requirements for a {@link Tag#key}.
   *
   * @param keyToValidate key to validate
   * @throws IllegalArgumentException if the key does not meet the requirements
   */
  protected static void validateKey(String keyToValidate) {
    Objects.requireNonNull(keyToValidate, "A tag's key cannot be null.");
    if (keyToValidate.length() > MAX_KEY_LENGTH) {
      throw new IllegalArgumentException(INVALID_KEY_LENGTH_EXCEPTION_MSG);
    }
    if (!keyToValidate.matches("[-.\\w\\d]+")) {
      throw new IllegalArgumentException(INVALID_KEY_STRING_EXCEPTION_MSG);
    }
  }
}
