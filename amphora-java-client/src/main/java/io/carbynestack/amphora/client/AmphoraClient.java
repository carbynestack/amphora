/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.MetadataPage;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.amphora.common.exceptions.SecretVerificationException;
import io.carbynestack.amphora.common.paging.PageRequest;
import io.carbynestack.amphora.common.paging.Sort;
import java.util.List;
import java.util.UUID;

/**
 * An interface for clients to communicate with the <i>Amphora</i> services from all parties
 * participating in the Carbyne Stack MPC cluster.
 *
 * <p>The interface describes all methods a client should provide in order to exploit all of the
 * services' functionalities. Such as create and retrieves secrets distributed as shares on multiple
 * amphora instances, or creating and updating {@link Tag}s used to describe an {@link Secret}'s
 * content.
 *
 * <p>The clients will provide all functionality to securely share and recombine a secret, based on
 * the MPC protocol used by the <i>Amphora</i> service.
 */
public interface AmphoraClient {

  /**
   * Uploads an {@link Secret} to the Amphora services of the MPC cluster.
   *
   * <p>The {@link Secret} is expected to be secret shared according to the used scheme.
   *
   * @param secret The secret to be secret shared and uploaded to the services
   * @return The id of the uploaded secret
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if there already exists a secret with the given id.
   */
  UUID createSecret(Secret secret) throws AmphoraClientException;

  /**
   * Deletes a distributed secret, referenced by the given id.
   *
   * @param secretId The id of the secret to delete
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if no secret with the given id exists.
   */
  void deleteSecret(UUID secretId) throws AmphoraClientException;

  /**
   * Retrieves the distributed secret by a given id.
   *
   * @param secretId The id of the secret
   * @return The requested secret
   * @throws SecretVerificationException if the vefification of the reassembled secret fails. This
   *     could either happen based on errors during transmition or at least one of the parties
   *     behaving dishonest.
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if no secret with the given id exists.
   */
  Secret getSecret(UUID secretId) throws AmphoraClientException;

  /**
   * Retrieves a list of all stored secret ids and their respective tags.
   *
   * @return A list of secret metadata. An empty list if there are no secrets available.
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  List<Metadata> getSecrets() throws AmphoraClientException;

  /**
   * Retrieves a list of all stored secret's metadata with the tags matching the given filter
   * criteria.
   *
   * @param filterCriteria A list of filters on tags to select only a subset of the stored secrets.
   *     If no filtering is desired, an empty list can be passed.
   * @return A list of secret metadata. An empty list if there are no secrets available.
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  List<Metadata> getSecrets(List<TagFilter> filterCriteria) throws AmphoraClientException;

  /**
   * Retrieves a page with a list of all stored secret's metadata.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * @param pageRequest Defines pagination and, if specified, sorting on the result. Only the first
   *     sort order is used.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  MetadataPage getSecrets(PageRequest pageRequest) throws AmphoraClientException;

  /**
   * Retrieves a page with a list of all stored secret's metadata.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * @param sort Sort configuration to be applied on the request.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  List<Metadata> getSecrets(Sort sort) throws AmphoraClientException;

  /**
   * Retrieves a page of a list of all stored secret's metadata with the tags matching the given
   * filter criteria.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * @param filterCriteria A list of filters on tags to select only a subset of the stored secrets.
   *     If no filtering is desired, an empty list can be passed.
   * @param sort Sort configuration to be applied on the request.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  List<Metadata> getSecrets(List<TagFilter> filterCriteria, Sort sort)
      throws AmphoraClientException;

  /**
   * Retrieves a page of a list of all stored secret's metadata with the tags matching the given
   * filter criteria.
   *
   * <p>This page will contain an empty secret list if there are no secrets available.
   *
   * @param filterCriteria A list of filters on tags to select only a subset of the stored secrets.
   *     If no filtering is desired, an empty list can be passed.
   * @param pageRequest Defines pagination and, if specified, sorting on the result. Only the first
   *     sort order is used.
   * @return A page containing a list of secret metadata
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed
   */
  MetadataPage getSecrets(List<TagFilter> filterCriteria, PageRequest pageRequest)
      throws AmphoraClientException;

  /**
   * Retrieves all tags associated with a secret stored in Amphora.
   *
   * @param secretId The id of the secret for which the tags will be retrieved
   * @return All tags associated with the secret
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if no secret with the given id exists.
   */
  List<Tag> getTags(UUID secretId) throws AmphoraClientException;

  /**
   * Adds a tag to a secret stored in Amphora.
   *
   * <p>This operation will fail if a {@link Tag} with the given key already exists.
   *
   * @param secretId The id of the secret to which the tag will be added
   * @param tag The tag to add
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if a {@link Tag} with the given key already exists.
   */
  void createTag(UUID secretId, Tag tag) throws AmphoraClientException;

  /**
   * Replaces all tags associated with a secret with new tags.
   *
   * @param secretId The id of the secret of which the tags will be overwritten
   * @param tags The tags which will replace the existing ones
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed or if no secret with the given id exists.
   */
  void overwriteTags(UUID secretId, List<Tag> tags) throws AmphoraClientException;

  /**
   * @param secretId The id of the secret for which the tag will be retrieved
   * @param tagKey The key of the requested tag
   * @return The secret's tag
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed, if no secret with the given id exists or if no tag with the
   *     given key exists.
   */
  Tag getTag(UUID secretId, String tagKey) throws AmphoraClientException;

  /**
   * Updates the value of a secret's tag. If a tag for the given key is not present at the secret,
   * it will be created.
   *
   * @param secretId The id of the secret for which the tag will be updated or created
   * @param tag The tag to update or create
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed, if no secret with the given id exists or if no tag with the
   *     given key exists.
   */
  void updateTag(UUID secretId, Tag tag) throws AmphoraClientException;

  /**
   * Deletes a tag from a secret.
   *
   * @param secretId The id of the secret from which the tag will be deleted
   * @param tagKey The key of the tag to delete
   * @throws AmphoraClientException if the communication with at least one of the defined
   *     <i>Amphora</i> services failed, if no secret with the given id exists or if no tag with the
   *     given key exists.
   */
  void deleteTag(UUID secretId, String tagKey) throws AmphoraClientException;
}
