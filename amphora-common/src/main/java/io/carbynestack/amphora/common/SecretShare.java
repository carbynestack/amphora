/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * An entity class representing a share of a secret.
 *
 * <p>General attributes describing the secret like {@link SecretShare#getSecretId() secretId} and
 * {@link SecretShare#getTags() tags} are inherited from {@link Metadata}.<br>
 * The actual content of the secret is stored in the gfp byte representation as used by the SPDZ
 * based MPC implementations. This data is stored in a byte array and must be of the exact word
 * length as defined by the {@link MpSpdzIntegrationUtils} (see {@link
 * MpSpdzIntegrationUtils#WORD_WIDTH}).<br>
 * A single {@link SecretShare} is of no value on its own and does not allow to gain any insights
 * into the original secret data shared across the virtual cloud.
 *
 * <p>New {@link SecretShare}s are created using the {@link SecretShare#builder() builder}.
 */
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Jacksonized
@SuperBuilder(toBuilder = true)
public final class SecretShare extends Metadata implements Serializable {
  private static final long serialVersionUID = -6504284210830334441L;
  public static final String INVALID_LENGTH_EXCEPTION_MSG =
      "Length of a SecretShare's data must e a multiple of %s bytes!";

  /**
   * The actual data of the {@link SecretShare} stored in the gfp byte representation as used by the
   * SPDZ based MPC implementations.<br>
   * This data is stored in a byte array and must be of the exact word length as defined by the
   * {@link MpSpdzIntegrationUtils} (see {@link MpSpdzIntegrationUtils#WORD_WIDTH}).
   */
  byte[] data;

  /**
   * A Builder for {@link SecretShare}.
   *
   * <p>A new {@link SecretShareBuilder} can be created using {@link SecretShare#builder()}. Each
   * attribute can be modified using the individual setter method.
   *
   * <p>This class is filled by <i>lombok</i> with its builder logic. Only {@link
   * SecretShareBuilder#data(byte[])} is pre-defined with custom logic.
   *
   * @param <C>
   * @param <B>
   */
  public abstract static class SecretShareBuilder<
          C extends SecretShare, B extends SecretShareBuilder<C, B>>
      extends MetadataBuilder<C, B> {
    /** The actual data of the {@link SecretShare}. */
    private byte[] data;

    /**
     * Sets the actual data of the {@link SecretShare}. (see {@link SecretShare#data})<br>
     * The data is expected to be in the gfp byte representation as used by the SPDZ based MPC
     * implementations and must be of the exact word length as defined by the {@link
     * MpSpdzIntegrationUtils} (see {@link MpSpdzIntegrationUtils#WORD_WIDTH}).
     *
     * @param data the {@link SecretShare}'s data
     * @return this {@link SecretShareBuilder builder}
     * @throws IllegalArgumentException if the data size is not a multiple of {@link
     *     MpSpdzIntegrationUtils#WORD_WIDTH}
     */
    public SecretShareBuilder<C, B> data(byte[] data) {
      this.data = data;
      return this;
    }
  }
}
