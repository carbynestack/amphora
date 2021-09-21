/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.Tag;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * An entity class representing an Amphora object. It has a globally unique identifier, some data
 * that can be secret shared, and tags.
 */
@Value
@AllArgsConstructor(staticName = "of")
public class Secret {

  /**
   * The identifier of the {@link Secret}.
   *
   * <p>Unique for a single party in the MPC CLuster, but shared across the participating parties in
   * order to match all related shares of an secret.
   */
  UUID secretId;
  /**
   * Metadata describing the content of the {@link Secret}.
   *
   * <p>These {@link Tag}s can be used for filtering and sorting e.g., when fetching a list of
   * available {@link Secret}s from the cluster or single parties.
   */
  List<Tag> tags;
  /**
   * The data of the secret.
   *
   * <p>This data will be secret shared according to the used secret sharing scheme.
   */
  BigInteger[] data;

  /**
   * Creates a new {@link Secret} with a random {@link Secret#secretId}.
   *
   * @param tags The {@link Secret#tags} of this {@link Secret}
   * @param data The {@link Secret#data} of this {@link Secret}
   * @return A new {@link Secret} with the given tags and data as well a a randomly assigned
   *     identifier.
   */
  public static Secret of(List<Tag> tags, BigInteger[] data) {
    return Secret.of(UUID.randomUUID(), tags, data);
  }

  /**
   * Returns the size (length) of the {@link Secret#data}.
   *
   * @return size of this {@link Secret#data secret's data}
   */
  public int size() {
    return data.length;
  }
}
