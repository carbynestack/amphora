/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.common.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class provides all resource paths and parameter names as exposed and used by the
 * AmphoraService
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmphoraRestApiEndpoints {

  /** Path for intra vcp operations */
  public static final String INTRA_VCP_OPERATIONS_SEGMENT = "/intra-vcp";

  /** Path for secure client up- and download operations */
  public static final String INTER_VCP_OPERATIONS_SEGMENT = "/inter-vcp";

  /** Endpoint to open interim values for multiplications */
  public static final String OPEN_INTERIM_VALUES_ENDPOINT = "/open";

  /** Access Secret Shares */
  public static final String SECRET_SHARES_ENDPOINT = "/secret-shares";

  /** Access Tags */
  public static final String TAGS_ENDPOINT = "/tags";

  /** Download Input Mask Shares */
  public static final String DOWNLOAD_INPUT_MASKS_ENDPOINT = "/input-masks";

  /** Upload Masked Inputs */
  public static final String UPLOAD_MASKED_INPUTS_ENDPOINT = "/masked-inputs";

  /** Parameter for specifying the number of requested items */
  public static final String COUNT_PARAMETER = "count";

  /** Parameter for specifying the family of requested items */
  public static final String FAMILY_PARAMETER = "shareFamily";

  /** Id to identify Secrets for download */
  public static final String SECRET_ID_PARAMETER = "secretId";

  /** Id for the input mask download request */
  public static final String REQUEST_ID_PARAMETER = "requestId";

  /** Key parameter for accessing a single tag */
  public static final String TAG_KEY_PARAMETER = "tagKey";

  /** Parameter for filtering tags */
  public static final String FILTER_PARAMETER = "filter";

  /** Separator for filter criteria */
  public static final String CRITERIA_SEPARATOR = ",";

  /** Parameter for the zero-based page index when using pagination */
  public static final String PAGE_NUMBER_PARAMETER = "pageNumber";

  /** Parameter for the page size to be returned when using pagination */
  public static final String PAGE_SIZE_PARAMETER = "pageSize";

  /** Parameter for the property (tag key) to sort */
  public static final String SORT_PROPERTY_PARAMETER = "sort";

  /** Parameter for the direction to sort (ASC,DESC) */
  public static final String SORT_DIRECTION_PARAMETER = "dir";
}
