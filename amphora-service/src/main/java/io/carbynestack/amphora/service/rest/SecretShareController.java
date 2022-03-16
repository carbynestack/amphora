/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.rest;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.*;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.calculation.OutputDeliveryService;
import io.carbynestack.amphora.service.exceptions.NotFoundException;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import io.vavr.control.Try;
import java.io.UnsupportedEncodingException;
import java.util.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = SECRET_SHARES_ENDPOINT)
@RequiredArgsConstructor
@Slf4j
public class SecretShareController {
  private final StorageService storageService;
  private final OutputDeliveryService outputDeliveryService;

  /**
   * Retrieves a page of all {@link Metadata} matching the given filters and criteria.
   *
   * <p>This method will return a single page containing all available secrets by default. If a
   * specific page number is requested but page size is omitted, a default page size of 1 will be
   * applied. If a page size is specified but no page number is defined, the first page (index 0)
   * will be returned by default. <br>
   * Content will be returned unsorted by default, unless specified.
   *
   * @param filter filter configuration to be applied
   * @param pageNumber specific page number to retrieve
   * @param pageSize number of entries per page
   * @param sortProperty the {@link Tag#getKey()} to sort on
   * @param sortDirection sort direction
   * @return a page of {@link Metadata}
   * @throws UnsupportedEncodingException if the given filters cannot be decoded
   */
  @GetMapping
  public ResponseEntity<MetadataPage> getObjectList(
      @RequestParam(required = false, value = FILTER_PARAMETER) String filter,
      @RequestParam(required = false, value = PAGE_NUMBER_PARAMETER, defaultValue = "0")
          int pageNumber,
      @RequestParam(required = false, value = PAGE_SIZE_PARAMETER, defaultValue = "0") int pageSize,
      @RequestParam(required = false, value = SORT_PROPERTY_PARAMETER) String sortProperty,
      @RequestParam(required = false, value = SORT_DIRECTION_PARAMETER) String sortDirection)
      throws UnsupportedEncodingException {
    List<TagFilter> tagFilters =
        StringUtils.isEmpty(filter) ? Collections.emptyList() : parseTagFilters(filter);
    Sort sort = getSort(sortProperty, sortDirection);
    Page<Metadata> secretSpringPage;
    if (isEmpty(tagFilters)) {
      secretSpringPage =
          (pageSize > 0 || pageNumber > 0)
              ? storageService.getSecretList(getPageRequest(pageNumber, pageSize, sort))
              : storageService.getSecretList(sort);
    } else {
      secretSpringPage =
          (pageSize > 0 || pageNumber > 0)
              ? storageService.getSecretList(tagFilters, getPageRequest(pageNumber, pageSize, sort))
              : storageService.getSecretList(tagFilters, sort);
    }
    return new ResponseEntity<>(
        new MetadataPage(
            secretSpringPage.getContent(),
            secretSpringPage.getNumber(),
            secretSpringPage.getSize(),
            secretSpringPage.getTotalElements(),
            secretSpringPage.getTotalPages()),
        new HttpHeaders(),
        HttpStatus.OK);
  }

  /**
   * Retrieves an {@link SecretShare} with a given id as {@link OutputDeliveryObject}
   *
   * @param secretId the id of the {@link SecretShare} to retrieve.
   * @param requestId a unique request id to link the get request across all parties of the CS MPC
   *     cluster.
   * @return the computed {@link OutputDeliveryObject}
   * @throws IllegalArgumentException if the request id was ommitted.
   * @throws AmphoraServiceException if the requested {@link SecretShare} exists but could not be
   *     retrieved.
   * @throws NotFoundException if no {@link SecretShare} with the given id exists
   */
  @GetMapping(path = "/{" + SECRET_ID_PARAMETER + "}")
  public ResponseEntity<OutputDeliveryObject> getSecretShare(
      @PathVariable final UUID secretId,
      @RequestParam(value = REQUEST_ID_PARAMETER) final UUID requestId) {
    Assert.notNull(requestId, "Request identifier must not be omitted");
    SecretShare secretShare = storageService.getSecretShare(secretId);
    return new ResponseEntity<>(
        outputDeliveryService.computeOutputDeliveryObject(secretShare, requestId), HttpStatus.OK);
  }

  /**
   * Deletes an {@link SecretShare} with a given id.
   *
   * @param secretId id of the {@link SecretShare} to delete.
   * @return {@link HttpStatus#OK} if successful
   * @throws NotFoundException if no {@link SecretShare} with the given id exists.
   * @throws AmphoraServiceException if the SecretEntity's data could not be deleted.
   */
  @DeleteMapping(path = "/{" + SECRET_ID_PARAMETER + "}")
  public ResponseEntity<Void> deleteSecretShare(@PathVariable UUID secretId) {
    storageService.deleteSecret(secretId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  Sort getSort(String sortProperty, String sortDirection) {
    if (!StringUtils.isEmpty(sortProperty)) {
      return Try.of(() -> Sort.Direction.fromString(sortDirection))
          .map(direction -> Sort.by(direction, sortProperty))
          .getOrElse(Sort.by(sortProperty));
    }
    return Sort.unsorted();
  }

  PageRequest getPageRequest(int pageNumber, int pageSize, @NonNull Sort sort) {
    return PageRequest.of(Math.max(0, pageNumber), Math.max(1, pageSize), sort);
  }

  List<TagFilter> parseTagFilters(String filter) throws UnsupportedEncodingException {
    List<TagFilter> tagFilters = new ArrayList<>();
    for (String tagFilterString : filter.split(CRITERIA_SEPARATOR)) {
      tagFilters.add(TagFilter.fromString(tagFilterString));
    }
    return tagFilters;
  }
}
