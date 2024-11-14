/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.exceptions.AlreadyExistsException;
import io.carbynestack.amphora.service.exceptions.NotFoundException;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.*;

@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RequestMapping(path = INTRA_VCP_OPERATIONS_SEGMENT + SECRET_SHARES_ENDPOINT)
public class IntraVcpController {
  private final StorageService storageService;

  /**
   * Upload and persist an {@link SecretShare}
   *
   * <p>{@link Tag}s that use a reserved tag {@link StorageService#RESERVED_TAG_KEYS} will be
   * removed before persisting the secret without further notice.
   *
   * @return {@link HttpStatus#CREATED} with a link to the stored {@link SecretShare} if successful.
   * @throws IllegalArgumentException if the {@link SecretShare}s could not be parsed / is null
   * @throws IllegalArgumentException if the given {@link Tag}s contain duplicate {@link
   *     Tag#getKey() keys}.
   * @throws AlreadyExistsException If there is already a Share with the given ID in the database
   */
  @PostMapping
  @Transactional
  public ResponseEntity<URI> uploadSecretShare(@RequestBody SecretShare secretShare) {
    Assert.notNull(secretShare, "SecretShare must not be null");
    return new ResponseEntity<>(
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .pathSegment(storageService.storeSecretShare(secretShare))
            .build()
            .toUri(),
        HttpStatus.CREATED);
  }

  /**
   * Retrieves an {@link SecretShare} with a given id.
   *
   * @param secretId id of the {@link SecretShare} to retrieve
   * @return {@link HttpStatus#OK} with the {@link SecretShare} if successful.
   * @throws AmphoraServiceException if an {@link SecretShare} exists but could not be retrieved.
   * @throws NotFoundException if no {@link SecretShare} with the given id exists
   */
  @GetMapping(path = "/{" + SECRET_ID_PARAMETER + "}")
  public ResponseEntity<SecretShare> downloadSecretShare(@PathVariable UUID secretId) {
    return new ResponseEntity<>(storageService.getSecretShareAuthorized(secretId), HttpStatus.OK);
  }
}
