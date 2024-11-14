/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.metadata;

import com.google.common.collect.Lists;
import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.calculation.SecretShareUtil;
import io.carbynestack.amphora.service.config.AmphoraServiceProperties;
import io.carbynestack.amphora.service.config.SpdzProperties;
import io.carbynestack.amphora.service.exceptions.AlreadyExistsException;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import io.carbynestack.amphora.service.exceptions.NotFoundException;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.opa.OpaService;
import io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService;
import io.carbynestack.amphora.service.persistence.datastore.SecretShareDataStore;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.carbynestack.amphora.service.opa.OpaService.OWNER_TAG_KEY;
import static io.carbynestack.amphora.service.persistence.metadata.TagEntity.setFromTagList;
import static io.carbynestack.amphora.service.persistence.metadata.TagEntity.setToTagList;

/**
 * A service to persist and manipulate {@link SecretEntity SecretEntities} and related data like
 * {@link TagEntity TagEntities} and {@link SecretShare SecretShares}.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class StorageService {
  public static final String CREATION_DATE_KEY = "creation-date";
  public static final List<String> RESERVED_TAG_KEYS = Lists.newArrayList(
          CREATION_DATE_KEY,
          OWNER_TAG_KEY);
  public static final String TAGS_WITH_THE_SAME_KEY_DEFINED_EXCEPTION_MSG =
          "Two or more tags with the same key defined.";
  public static final String SECRET_WITH_ID_EXISTS_EXCEPTION_MSG =
          "A secret with the given id already exists.";
  public static final String IS_RESERVED_KEY_EXCEPTION_MSG = "\"%s\" is a reserved key.";
  public static final String NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG =
          "No secret with the given id #%s exists.";
  public static final String NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG =
          "No tag with key \"%s\" exists for secret with id #%s.";
  public static final String TAG_WITH_KEY_EXISTS_FOR_SECRET_EXCEPTION_MSG =
          "A tag with key \"%s\" already exists for secret #%s";

  private final SecretEntityRepository secretEntityRepository;
  private final InputMaskCachingService inputMaskStore;
  private final TagRepository tagRepository;
  private final SecretShareUtil secretShareUtil;
  private final SpdzProperties spdzProperties;
  private final AmphoraServiceProperties amphoraServiceProperties;
  private final SecretShareDataStore secretShareDataStore;
  private final OpaService opaService;

  /**
   * Takes a {@link MaskedInput}, converts it into an individual {@link SecretShare} and persits the
   * date and tags.
   *
   * <p>{@link Tag}s that use a reserved tag {@link #RESERVED_TAG_KEYS} will be removed before
   * persisting the secret without further notice.
   *
   * @param maskedInput the {@link MaskedInput} to persist
   * @param authorizedUserId the id of the authenticated user becoming the owner of the persisted {@link SecretShare}
   * @return the id of the new {@link SecretShare} as {@link String}
   * @throws AlreadyExistsException if an {@link SecretShare} with the given id already exists.
   * @throws IllegalArgumentException if one or more {@link Tag}s with the same {@link Tag#getKey()
   *     key} are defined.
   * @throws AmphoraServiceException if storing the {@link SecretShare#getData() data} fails.
   * @throws AmphoraServiceException if retrieving the {@link InputMask}s for the given request
   *     fails
   */
  @Transactional
  public String createSecret(MaskedInput maskedInput, String authorizedUserId) {
    if (secretEntityRepository.existsById(maskedInput.getSecretId().toString())) {
      throw new AlreadyExistsException(SECRET_WITH_ID_EXISTS_EXCEPTION_MSG);
    }
    if (hasDuplicateKey(maskedInput.getTags())) {
      throw new IllegalArgumentException(TAGS_WITH_THE_SAME_KEY_DEFINED_EXCEPTION_MSG);
    }
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMasks =
            inputMaskStore.getCachedInputMasks(maskedInput.getSecretId());
    SecretShare secretShare =
            secretShareUtil.convertToSecretShare(
                    maskedInput,
                    spdzProperties.getMacKey(),
                    inputMasks,
                    amphoraServiceProperties.getPlayerId() != 0);
    String secretId = persistSecretShare(secretShare,
            Collections.singletonList(Tag.builder().key(OWNER_TAG_KEY).value(authorizedUserId).build()));
    inputMaskStore.removeInputMasks(secretShare.getSecretId());
    return secretId;
  }

  /**
   * Persists an {@link SecretShare}.
   *
   * <p>{@link Tag}s that use a reserved tag {@link #RESERVED_TAG_KEYS} will be removed before
   * persisting the secret without further notice.
   *
   * @param secretShare the {@link SecretShare} to store.
   * @return the id of the new {@link SecretShare} as {@link String}
   * @throws AlreadyExistsException if an {@link SecretShare} with the given id already exists.
   * @throws IllegalArgumentException if one or more {@link Tag}s with the same {@link Tag#getKey()
   *     key} are defined.
   * @throws AmphoraServiceException if storing the {@link SecretShare#getData() data} fails.
   */
  @Transactional
  public String storeSecretShare(SecretShare secretShare) {
    if (secretEntityRepository.existsById(secretShare.getSecretId().toString())) {
      throw new AlreadyExistsException(SECRET_WITH_ID_EXISTS_EXCEPTION_MSG);
    }
    if (hasDuplicateKey(secretShare.getTags())) {
      throw new IllegalArgumentException(TAGS_WITH_THE_SAME_KEY_DEFINED_EXCEPTION_MSG);
    }
    return persistSecretShare(secretShare, Collections.emptyList());
  }

  /**
   * @return the id used to reference the persisted data
   * @throws AmphoraServiceException if storing the {@link SecretShare#getData() data} fails.
   */
  private String persistSecretShare(SecretShare secretShare, List<Tag> reservedTags) {
    List<Tag> tags = dropReservedTags(new ArrayList<>(secretShare.getTags()));
    tags.add(
            Tag.builder()
                    .key(StorageService.CREATION_DATE_KEY)
                    .value(Long.toString(System.currentTimeMillis()))
                    .build());
    tags.addAll(reservedTags);
    Set<TagEntity> tagEntities = setFromTagList(tags);
    String persistedSecretId =
            secretEntityRepository
                    .save(new SecretEntity(secretShare.getSecretId().toString(), tagEntities))
                    .getSecretId();
    secretShareDataStore.storeSecretShareData(
            UUID.fromString(persistedSecretId), secretShare.getData());
    return persistedSecretId;
  }

  private static boolean hasDuplicateKey(List<Tag> tags) {
    if (CollectionUtils.isEmpty(tags)) {
      return false;
    }
    Set<String> set = new HashSet<>(tags.size());
    for (Tag tag : tags) {
      if (!set.add(tag.getKey())) {
        return true;
      }
    }
    return false;
  }

  private static List<Tag> dropReservedTags(List<Tag> tags) {
    if (!CollectionUtils.isEmpty(tags)) {
      List<Tag> itemsToDrop =
              tags.stream().filter(StorageService::tagIsReserved).collect(Collectors.toList());
      itemsToDrop.forEach(
              t -> {
                log.debug("Dropped tag {} for using reserved key.", t.toString());
                tags.remove(t);
              });
    }
    return tags;
  }

  public static boolean tagIsReserved(Tag t) {
    return RESERVED_TAG_KEYS.contains(t.getKey());
  }

  @Transactional(readOnly = true)
  public Page<Metadata> getSecretList(Pageable pageable) {
    return secretEntityRepository.findAll(pageable).map(SecretEntity::toMetadata);
  }

  @Transactional(readOnly = true)
  public Page<Metadata> getSecretList(Sort sort) {
    return getSecretList(PageRequest.of(0, Integer.MAX_VALUE, sort));
  }

  @Transactional(readOnly = true)
  public Page<Metadata> getSecretList(List<TagFilter> tagFilters, Pageable pageable) {
    return secretEntityRepository
            .findAll(SecretEntitySpecification.with(tagFilters), pageable)
            .map(SecretEntity::toMetadata);
  }

  @Transactional(readOnly = true)
  public Page<Metadata> getSecretList(List<TagFilter> tagFilters, Sort sort) {
    return getSecretList(tagFilters, PageRequest.of(0, Integer.MAX_VALUE, sort));
  }

  /**
   * Retrieves an {@link SecretShare} with a given id.
   *
   * @param secretId id of the {@link SecretShare} to retrieve
   * @return an {@link Option} containing the requested {@link SecretShare} of {@link Option#none()}
   *     if no secret with the given id exists.
   * @throws AmphoraServiceException if an {@link SecretShare} exists but could not be retrieved.
   * @throws NotFoundException if no {@link SecretShare} with the given id exists
   */
  @Transactional(readOnly = true)
  public SecretShare getSecretShareAuthorized(UUID secretId) {
    return secretEntityRepository
            .findById(secretId.toString())
            .map(this::getSecretShareForEntity)
            .orElseThrow(
                    () ->
                            new NotFoundException(
                                    String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
  }

  /**
   * Retrieves an {@link SecretShare} with a given id.
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canReadSecret(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   *
   * @param secretId id of the {@link SecretShare} to retrieve
   * @param authorizedUserId the id of the authenticated user requesting the operation
   * @return an {@link Option} containing the requested {@link SecretShare} of {@link Option#none()}
   *     if no secret with the given id exists.
   * @throws AmphoraServiceException if an {@link SecretShare} exists but could not be retrieved.
   * @throws NotFoundException if no {@link SecretShare} with the given id exists
   * @throws UnauthorizedException if the user not authorized to read the secret
   * @throws CsOpaException if an error occurred while evaluating the policy
   */
  @Transactional(readOnly = true)
  public SecretShare getSecretShare(UUID secretId, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    SecretEntity secretEntity = secretEntityRepository
            .findById(secretId.toString())
            .orElseThrow(
                    () ->
                            new NotFoundException(
                                    String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));

    if(!opaService.canReadSecret(authorizedUserId, setToTagList(secretEntity.getTags()))) {
      throw new UnauthorizedException("User is not authorized to read this secret");
    }
    return getSecretShareForEntity(secretEntity);
  }

  private SecretShare getSecretShareForEntity(SecretEntity entity) {
    return SecretShare.builder()
            .secretId(UUID.fromString(entity.getSecretId()))
            .data(secretShareDataStore.getSecretShareData(UUID.fromString(entity.getSecretId())))
            .tags(setToTagList(entity.getTags()))
            .build();
  }

  /**
   * Removes an {@link SecretEntity} and all related information ({@link TagEntity tags} and {@link
   * SecretShare data}) from the storage.
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canDeleteSecret(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   *
   * @param secretId the id of the secret to be removed.
   * @param authorizedUserId the id of the authenticated user requesting the deletion
   * @throws NotFoundException if no {@link SecretEntity} with the given id exists.
   * @throws AmphoraServiceException if the {@link SecretEntity}'s data could not be deleted.
   * @throws UnauthorizedException if the user is not authorized to delete the secret
   * @throws CsOpaException if an error occurred while evaluating the policy
   */
  @Transactional
  public void deleteSecret(UUID secretId, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    // Better to accept String as input once - instead of repeatedly converting it.
    SecretEntity secretEntity = secretEntityRepository.findById(secretId.toString())
            .orElseThrow(() ->
                  new NotFoundException(
                          String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
    if(!opaService.canDeleteSecret(authorizedUserId, setToTagList(secretEntity.getTags()))) {
        throw new UnauthorizedException("User is not authorized to delete this secret");
    }
    secretEntityRepository.deleteBySecretId(secretId.toString());
    secretShareDataStore.deleteSecretShareData(secretId);
  }

  /**
   * Persists a {@link Tag} related to a specified {@link SecretShare}
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canCreateTags(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   *
   * @param secretId id of the secret this {@link Tag} belongs to
   * @param tag the tag to persist
   * @param authorizedUserId the ID of the authenticated user requesting the operation
   * @return the {@link Tag#getKey() key} of the stored {@link Tag}
   * @throws IllegalArgumentException if tag uses a reserved key (see {@link #RESERVED_TAG_KEYS}).
   * @throws NotFoundException if no {@link SecretEntity} with the given id exists.
   * @throws AlreadyExistsException if a {@link Tag} with the same {@link Tag#getKey() key} already
   *     exists.
   */
  @Transactional
  public String storeTag(UUID secretId, Tag tag, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    if (tagIsReserved(tag)) {
      throw new IllegalArgumentException(
              String.format(IS_RESERVED_KEY_EXCEPTION_MSG, tag.getKey()));
    }
    SecretEntity secretEntity =
            secretEntityRepository
                    .findById(secretId.toString())
                    .orElseThrow(
                            () ->
                                    new NotFoundException(
                                            String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
    if (!opaService.canCreateTags(authorizedUserId, setToTagList(secretEntity.getTags()))) {
        throw new UnauthorizedException("User is not authorized to create tags for this secret");
    }
    tagRepository
            .findBySecretAndKey(secretEntity, tag.getKey())
            .ifPresent(
                    t -> {
                      throw new AlreadyExistsException(
                              String.format(
                                      TAG_WITH_KEY_EXISTS_FOR_SECRET_EXCEPTION_MSG, tag.getKey(), secretId));
                    });
    return tagRepository.save(TagEntity.fromTag(tag).setSecret(secretEntity)).getKey();
  }

  /**
   * Replaces the {@link Tag}s for a {@link SecretEntity} with the given id.
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canUpdateTags(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   * <p>
   * {@link Tag}s that use a reserved tag {@link #RESERVED_TAG_KEYS} will be removed before
   * persisting the secret without further notice.
   *
   * @param secretId the id of the {@link SecretEntity} whose tags should be replaced.
   * @param tags the new set of {@link Tag}s.
   * @param authorizedUserId the id of the authenticated user requesting the operation
   * @throws IllegalArgumentException if the set of {@link Tag}s contains duplicate {@link
   *     Tag#getKey() keys}.
   * @throws NotFoundException if no {@link SecretEntity} with the given id exists.
   * @throws UnauthorizedException if the user is not authorized to update the tags
   * @throws CsOpaException if an error occurred while evaluating the policy
   */
  @Transactional
  public void replaceTags(UUID secretId, List<Tag> tags, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    if (hasDuplicateKey(tags)) {
      throw new IllegalArgumentException(TAGS_WITH_THE_SAME_KEY_DEFINED_EXCEPTION_MSG);
    }
    SecretEntity secretEntityReference = secretEntityRepository.findById(secretId.toString())
            .orElseThrow(() ->
                    new NotFoundException(
                            String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
    if (!opaService.canUpdateTags(authorizedUserId, setToTagList(secretEntityReference.getTags()))) {
        throw new UnauthorizedException("User is not authorized to update tags for this secret");
    }
    List<TagEntity> existingReservedTags =
            RESERVED_TAG_KEYS.stream()
                    .map(key -> tagRepository.findBySecretAndKey(secretEntityReference, key))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
    List<Tag> newTags = dropReservedTags(new ArrayList<>(tags));
    tagRepository.deleteBySecret(secretEntityReference);
    Set<TagEntity> newTagList = setFromTagList(newTags);
    newTagList.forEach(t -> t.setSecret(secretEntityReference));
    newTagList.addAll(existingReservedTags);
    tagRepository.saveAll(newTagList);
  }

  /**
   * Returns all {@link Tag}s associated to an {@link SecretEntity} with the given id.
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canReadTags(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   *
   * @param secretId the id of the {@link SecretEntity} whose tags should be retrieved.
   * @param authorizedUserId the id of the authenticated user requesting the operation
   * @return a list of {@link Tag}s
   * @throws NotFoundException if no {@link SecretEntity} with the given id exists.
   * @throws UnauthorizedException if the user is not authorized to read the tags
   * @throws CsOpaException if an error occurred while evaluating the policy
   */
  @Transactional(readOnly = true)
  public List<Tag> retrieveTags(UUID secretId, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    SecretEntity secretEntity =
            secretEntityRepository
                    .findById(secretId.toString())
                    .orElseThrow(
                            () ->
                                    new NotFoundException(
                                            String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
    if (!opaService.canReadTags(authorizedUserId, setToTagList(secretEntity.getTags()))) {
        throw new UnauthorizedException("User is not authorized to read tags for this secret");
    }
    return setToTagList(secretEntity.getTags());
  }

  /**
   * Returns a single {@link Tag}s associated to an {@link SecretEntity} with the given id, and a
   * specified {@link Tag#getKey() key}.
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canReadTags(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   *
   * @param secretId the id of the {@link SecretShare} whose {@link Tag} to be retrieved.
   * @param key the {@link Tag#getKey() key} of the {@link Tag} to be retrieved.
   * @param authorizedUserId the id of the authenticated user requesting the operation
   * @return the {@link Tag}
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws NotFoundException if no {@link Tag} with the given {@link Tag#getKey() key} exists.
   * @throws UnauthorizedException if the user is not authorized to read the tag
   * @throws CsOpaException if an error occurred while evaluating the policy
   */
  @Transactional(readOnly = true)
  public Tag retrieveTag(UUID secretId, String key, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    SecretEntity secretEntityReference = secretEntityRepository.findById(secretId.toString())
            .orElseThrow(() ->
                    new NotFoundException(
                            String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
    if (!opaService.canReadTags(authorizedUserId, setToTagList(secretEntityReference.getTags()))) {
        throw new UnauthorizedException("User is not authorized to read tags for this secret");
    }
    return tagRepository
            .findBySecretAndKey(secretEntityReference, key)
            .map(TagEntity::toTag)
            .orElseThrow(
                    () ->
                            new NotFoundException(
                                    String.format(
                                            NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG, key, secretId)));
  }

  /**
   * Updates an existing {@link Tag} linked to an {@link SecretEntity} with the given id.
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canUpdateTags(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   *
   * @param secretId the id of the {@link SecretShare} whose {@link Tag} to be updated.
   * @param tag the new tag
   * @param authorizedUserId the id of the authenticated user requesting the operation
   * @throws IllegalArgumentException if tag uses a reserved key (see {@link #RESERVED_TAG_KEYS}).
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws NotFoundException if no {@link Tag} with the given {@link Tag#getKey() key} exists.
   * @throws UnauthorizedException if the user is not authorized to update the tag
   * @throws CsOpaException if an error occurred while evaluating the policy
   */
  @Transactional
  public void updateTag(UUID secretId, Tag tag, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    if (tagIsReserved(tag)) {
      throw new IllegalArgumentException(
              String.format(IS_RESERVED_KEY_EXCEPTION_MSG, tag.getKey()));
    }
    SecretEntity secretEntityReference = secretEntityRepository.findById(secretId.toString())
            .orElseThrow(() ->
                    new NotFoundException(
                            String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
    if (!opaService.canUpdateTags(authorizedUserId, setToTagList(secretEntityReference.getTags()))) {
        throw new UnauthorizedException("User is not authorized to update tags for this secret");
    }
    TagEntity existingTag =
            tagRepository
                    .findBySecretAndKey(secretEntityReference, tag.getKey())
                    .orElseThrow(
                            () ->
                                    new NotFoundException(
                                            String.format(
                                                    NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG,
                                                    tag.getKey(),
                                                    secretId)));
    tagRepository.save(
            existingTag.setValue(tag.getValue()).setValueType(tag.getValueType().toString()));
  }

  /**
   * Deletes an existing {@link Tag} linked to an {@link SecretEntity} with the given id.
   * <p>
   * This method requires the authenticated user to be authorized to perform the operation. The authorization is
   * checked by the {@link OpaService#canDeleteTags(String, List)} method using the {@link Tag}s of
   * the related {@link SecretEntity}.
   *
   * @param secretId the id of the {@link SecretShare} whose {@link Tag} to be deleted.
   * @param key the {@link Tag#getKey() key} of the {@link Tag} to be deleted.
   * @param authorizedUserId the id of the authenticated user requesting the operation
   * @throws IllegalArgumentException if tag uses a reserved key (see {@link #RESERVED_TAG_KEYS}).
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws NotFoundException if no {@link Tag} with the given {@link Tag#getKey() key} exists.
   * @throws UnauthorizedException if the user is not authorized to delete the tag
   * @throws CsOpaException if an error occurred while evaluating the policy
   */
  @Transactional
  public void deleteTag(UUID secretId, String key, String authorizedUserId) throws CsOpaException, UnauthorizedException {
    if (RESERVED_TAG_KEYS.contains(key)) {
      throw new IllegalArgumentException(String.format(IS_RESERVED_KEY_EXCEPTION_MSG, key));
    }
    SecretEntity secretEntityReference = secretEntityRepository.findById(secretId.toString())
            .orElseThrow(() ->
                    new NotFoundException(
                            String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));

    if (!opaService.canDeleteTags(authorizedUserId, setToTagList(secretEntityReference.getTags()))) {
      throw new UnauthorizedException("User is not authorized to delete tags for this secret");
    }
    secretEntityReference.getTags().remove(tagRepository
            .findBySecretAndKey(secretEntityReference, key).orElseThrow(
                    () ->
                            new NotFoundException(
                                    String.format(
                                            NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG,
                                            key,
                                            secretId))));
//    SecretEntity secretEntityReference = secretEntityRepository.findById(secretId.toString())
//            .orElseThrow(() ->
//                    new NotFoundException(
//                            String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, secretId)));
//    if (!opaService.canDeleteTags(authorizedUserId, setToTagList(secretEntityReference.getTags()))) {
//      throw new UnauthorizedException("User is not authorized to delete tags for this secret");
//    }
//    TagEntity tagEntity = tagRepository.findBySecretAndKey(secretEntityReference, key)
//            .orElseThrow(() ->
//                    new NotFoundException(
//                            String.format(
//                                    NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG,
//                                    key,
//                                    secretId)));
//    tagRepository.delete(tagEntity);
  }
}
