/*
 * Copyright (c) 2021-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.exceptions.AlreadyExistsException;
import io.carbynestack.amphora.service.exceptions.NotFoundException;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.castor.common.exceptions.CastorServiceException;
import java.io.UnsupportedEncodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestControllerExceptionHandler extends ResponseEntityExceptionHandler {
  private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writer();

  @ExceptionHandler(NotFoundException.class)
  protected ResponseEntity<Object> handleNotFoundException(NotFoundException nfe)
      throws JsonProcessingException {
    logger.debug("Handling NotFound Error", nfe);
    return new ResponseEntity<>(
        OBJECT_WRITER.writeValueAsString(nfe.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(AlreadyExistsException.class)
  protected ResponseEntity<String> handleAlreadyExistException(AlreadyExistsException aee)
      throws JsonProcessingException {
    logger.debug("Handling AlreadyExists Error", aee);
    return new ResponseEntity<>(
        OBJECT_WRITER.writeValueAsString(aee.getMessage()), HttpStatus.CONFLICT);
  }

  @ExceptionHandler({UnsupportedEncodingException.class, IllegalArgumentException.class})
  protected ResponseEntity<String> handleBadRequestException(Exception e)
      throws JsonProcessingException {
    logger.debug("Handling BadRequest Error", e);
    return new ResponseEntity<>(
        OBJECT_WRITER.writeValueAsString(e.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({UnauthorizedException.class})
  protected ResponseEntity<String> handleUnauthorizedException(UnauthorizedException e)
      throws JsonProcessingException {
    logger.debug("Handling Unauthorized Error", e);
    return new ResponseEntity<>(
        OBJECT_WRITER.writeValueAsString(e.getMessage()), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler({AmphoraServiceException.class, CastorServiceException.class, Exception.class})
  protected ResponseEntity<String> handleInternalException(Exception e)
      throws JsonProcessingException {
    logger.debug("Handling Server Error", e);
    return new ResponseEntity<>(
        OBJECT_WRITER.writeValueAsString(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
