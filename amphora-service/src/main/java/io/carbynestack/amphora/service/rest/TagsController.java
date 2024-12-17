/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.*;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.exceptions.AlreadyExistsException;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import io.carbynestack.amphora.service.exceptions.NotFoundException;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.opa.JwtReader;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RequestMapping(path = SECRET_SHARES_ENDPOINT + "/{" + SECRET_ID_PARAMETER + "}" + TAGS_ENDPOINT)
public class TagsController {
  private final StorageService storageService;
  private final JwtReader jwtReader;

  /**
   * Retrieves all {@link Tag}s for an {@link SecretShare} with the given id.
   *
   * @param secretId the id of the {@link SecretShare} whose {@link Tag}s are to be retrieved.
   * @return {@link HttpStatus#OK} and the list of retrieved {@link Tag}s.
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   */
  @GetMapping
  public ResponseEntity<List<Tag>> getTags(
      @RequestHeader("Authorization") String authorizationHeader, @PathVariable UUID secretId)
      throws UnauthorizedException, CsOpaException {
    return new ResponseEntity<>(
        storageService.retrieveTags(
            secretId, jwtReader.extractUserIdFromAuthHeader(authorizationHeader)),
        HttpStatus.OK);
  }

  /**
   * Stores a new {@link Tag} for an {@link SecretShare} with the given id.
   *
   * @param secretId the if if the {@link SecretShare} to which this {@link Tag} belongs to.
   * @param tag the new {@link Tag} to be stored.
   * @return {@link HttpStatus#CREATED} with a link to the stored {@link Tag} if successful.
   * @throws IllegalArgumentException if the Tag cannot be parsed / is null.
   * @throws IllegalArgumentException if tag uses a reserved key (see {@link
   *     StorageService#RESERVED_TAG_KEYS}).
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws AlreadyExistsException if a Tag with the same key already exists for the given secret.
   */
  @Transactional
  @PostMapping
  public ResponseEntity<URI> createTag(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable UUID secretId,
      @RequestBody Tag tag)
      throws UnauthorizedException, CsOpaException {
    Assert.notNull(tag, "Tag must not be empty");
    return new ResponseEntity<>(
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .pathSegment(
                storageService.storeTag(
                    secretId, tag, jwtReader.extractUserIdFromAuthHeader(authorizationHeader)))
            .build()
            .toUri(),
        HttpStatus.CREATED);
  }

  /**
   * Replaces the {@link Tag}s for a given {@link SecretShare}.
   *
   * <p>{@link Tag}s that use a reserved tag {@link StorageService#RESERVED_TAG_KEYS} will be
   * removed before persisting the secret without further notice.
   *
   * @param secretId the id of the {@link SecretShare} the {@link Tag} belongs to
   * @param tags the new {@link Tag}s
   * @return {@link HttpStatus#OK} with a link to the updated {@link SecretShare} if successful
   * @throws IllegalArgumentException if the {@link Tag}s cannot be parsed / is null or empty
   * @throws IllegalArgumentException if the given {@link Tag}s contain duplicate {@link
   *     Tag#getKey() keys}.
   * @throws NotFoundException if no {@link SecretShare} with the given id exists
   */
  @Transactional
  @PutMapping
  public ResponseEntity<Void> updateTags(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable UUID secretId,
      @RequestBody List<Tag> tags)
      throws UnauthorizedException, CsOpaException {
    Assert.notEmpty(tags, "At least one tag must be given.");
    storageService.replaceTags(
        secretId, tags, jwtReader.extractUserIdFromAuthHeader(authorizationHeader));
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Retrieves a single {@link Tag} from an {@link SecretShare} with the given id.
   *
   * @param secretId the id of the {@link SecretShare} whose {@link Tag}s to be retrieved.
   * @param tagKey the {@link Tag#getKey() key} of the {@link Tag} to be retrieved.
   * @return {@link HttpStatus#OK} with the requested {@link Tag} when successful.
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws NotFoundException if no {@link Tag} with the given {@link Tag#getKey() key} exists.
   */
  @GetMapping(path = "/{" + TAG_KEY_PARAMETER + ":.+}")
  public ResponseEntity<Tag> getTag(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable UUID secretId,
      @PathVariable String tagKey)
      throws UnauthorizedException, CsOpaException {
    return new ResponseEntity<>(
        storageService.retrieveTag(
            secretId, tagKey, jwtReader.extractUserIdFromAuthHeader(authorizationHeader)),
        HttpStatus.OK);
  }

  /**
   * Updates a single {@link Tag} from an {@link SecretShare} with the given id.
   *
   * @param secretId the id of the {@link SecretShare} whose {@link Tag} to be updated.
   * @param tagKey the {@link Tag#getKey() key} of the {@link Tag} to be updated.
   * @param tag the new {@link Tag} data
   * @return {@link HttpStatus#OK} when successful.
   * @throws IllegalArgumentException if the {@link Tag}s cannot be parsed / is null
   * @throws IllegalArgumentException if the given tag key does not equal to the key defined in the
   *     given tag.
   * @throws IllegalArgumentException if tag uses a reserved key (see {@link
   *     StorageService#RESERVED_TAG_KEYS}).
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws NotFoundException if no {@link Tag} with the given {@link Tag#getKey() key} exists.
   */
  @Transactional
  @PutMapping(path = "/{" + TAG_KEY_PARAMETER + ":.+}")
  public ResponseEntity<Void> putTag(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable UUID secretId,
      @PathVariable String tagKey,
      @RequestBody Tag tag)
      throws UnauthorizedException, CsOpaException {
    Assert.notNull(tag, "Tag must not be empty");
    if (!tagKey.equals(tag.getKey())) {
      throw new IllegalArgumentException(
          String.format("The defined key and tag data do not match.%n%s <> %s", tagKey, tag));
    }
    storageService.updateTag(
        secretId, tag, jwtReader.extractUserIdFromAuthHeader(authorizationHeader));
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Deletes a {@link Tag} from an {@link SecretShare} with a given id.
   *
   * @param secretId the id of the {@link SecretShare} whose {@link Tag} to be deleted.
   * @param tagKey the {@link Tag#getKey() key} of the {@link Tag} to be deleted.
   * @return {@link HttpStatus#OK} when successful.
   * @throws IllegalArgumentException if referenced tag has a reserved key (see {@link
   *     StorageService#RESERVED_TAG_KEYS}).
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws NotFoundException if no {@link Tag} with the given {@link Tag#getKey() key} exists.
   */
  @Transactional
  @DeleteMapping(path = "/{" + TAG_KEY_PARAMETER + ":.+}")
  public ResponseEntity<Void> deleteTag(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable UUID secretId,
      @PathVariable String tagKey)
      throws UnauthorizedException, CsOpaException {
    storageService.deleteTag(
        secretId, tagKey, jwtReader.extractUserIdFromAuthHeader(authorizationHeader));
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
