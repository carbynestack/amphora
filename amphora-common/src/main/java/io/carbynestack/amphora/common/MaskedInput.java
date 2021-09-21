/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.common;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.Value;

/**
 * A {@link MaskedInput} is used to securely secret share values with the participating parties. The
 * initial secret value is therefore masked by the client using so called input masks. Each party
 * will then use the {@link MaskedInput} to calculate its individual share of the secret
 * information, which however does not reveal any information about the initial secret, and store
 * this share referenced by the given unique id and the provided tags.
 */
@Value
public class MaskedInput implements Serializable {
  private static final long serialVersionUID = 6635586440531984234L;

  /**
   * An identifier used to match the distributed shares of a secret amongst the participating
   * parties.
   *
   * <p>The identifier must not be <i>null</i>
   */
  UUID secretId;
  /**
   * The masked secret that will be uploaded and shared amongst the involved parties.
   *
   * <p>It does not reveal any information about the initial secret.
   */
  List<MaskedInputData> data;
  /**
   * A list of metadata used to describe the content of the shared information. <br>
   * This data can be used for further processing, e.g., to filter all shared secrets based on
   * specific characteristics. <br>
   * See {@link Tag} for further information like restricted keys or maximum value length.
   */
  List<Tag> tags;

  /**
   * Creates a new {@link MaskedInput}
   *
   * @param secretId The identifier of the {@link MaskedInput}
   * @param data The masked secret data
   * @param tags Tags used to describe the content of the secret
   * @throws NullPointerException if the given identifier is <i>null</i>
   */
  @ConstructorProperties({"secretId", "data", "tags"})
  public MaskedInput(@NonNull UUID secretId, List<MaskedInputData> data, List<Tag> tags) {
    this.secretId = secretId;
    this.data = data;
    this.tags = tags == null ? new ArrayList<>() : tags;
  }
}
