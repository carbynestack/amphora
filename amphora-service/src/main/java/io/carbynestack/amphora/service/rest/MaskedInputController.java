/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.UPLOAD_MASKED_INPUTS_ENDPOINT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

import io.carbynestack.amphora.common.MaskedInput;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.exceptions.AlreadyExistsException;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import java.net.URI;

import io.carbynestack.amphora.service.opa.JwtReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RequestMapping(path = UPLOAD_MASKED_INPUTS_ENDPOINT)
public class MaskedInputController {
  private final StorageService storageService;
  private final JwtReader jwtReader;

  /**
   * Takes a {@link MaskedInput}, converts it into an individual {@link SecretShare} and persits the
   * date and tags.
   *
   * <p>{@link Tag}s that use a reserved tag {@link StorageService#RESERVED_TAG_KEYS} will be
   * removed before persisting the secret without further notice.
   *
   * @param maskedInput the {@link MaskedInput} to persist
   * @return {@link HttpStatus#CREATED} and a link to the generated {@link SecretShare} if
   *     successful
   * @throws IllegalArgumentException if maskedInput is empty or is malformed / could not be parsed
   * @throws IllegalArgumentException if one or more {@link Tag}s with the same {@link Tag#getKey()
   *     key} are defined.
   * @throws AlreadyExistsException if an {@link SecretShare} with the given id already exists.
   */
  @PostMapping
  public ResponseEntity<URI> upload(@RequestHeader("Authorization") String authorizationHeader,
                                    @RequestBody MaskedInput maskedInput) throws UnauthorizedException {
    notNull(maskedInput, "MaskedInput must not be null");
    notEmpty(maskedInput.getData(), "MaskedInput data must not be empty");
    return new ResponseEntity<>(
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .pathSegment(
                    storageService.createSecret(
                            maskedInput,
                            jwtReader.extractUserIdFromAuthHeader(authorizationHeader)))
            .build()
            .toUri(),
        CREATED);
  }
}
