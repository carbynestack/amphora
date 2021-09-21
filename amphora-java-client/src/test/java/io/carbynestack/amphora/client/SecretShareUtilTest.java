/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import io.carbynestack.amphora.common.exceptions.SecretVerificationException;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class SecretShareUtilTest {
  private final Random rnd = new Random(42);

  private final SecretShareUtil secretShareUtil =
      SecretShareUtil.of(
          new BigInteger("198766463529478683931867765928436695041"),
          new BigInteger("141515903391459779531506841503331516415"),
          new BigInteger("133854242216446749056083838363708373830"));

  @Test
  public void givenAllValid_whenVerifyingSecrets_thenDontThrowException() {
    int numberOfValues = 5;
    List<BigInteger> secrets =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> BigInteger.valueOf(Math.abs(rnd.nextLong())))
            .collect(Collectors.toList());
    List<BigInteger> rs =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> BigInteger.valueOf(Math.abs(rnd.nextLong())))
            .collect(Collectors.toList());
    List<BigInteger> vs =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> BigInteger.valueOf(Math.abs(rnd.nextLong())))
            .collect(Collectors.toList());
    List<BigInteger> ws =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> secrets.get(i).multiply(rs.get(i)))
            .collect(Collectors.toList());
    List<BigInteger> us =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> vs.get(i).multiply(rs.get(i)))
            .collect(Collectors.toList());
    secretShareUtil.verifySecrets(secrets, rs, us, vs, ws);
  }

  @Test
  public void givenWsContainingInvalidValue_whenVerifyingSecrets_thenThrowException() {
    int numberOfValues = 5;
    List<BigInteger> secrets =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> BigInteger.valueOf(Math.abs(rnd.nextLong())))
            .collect(Collectors.toList());
    List<BigInteger> rs =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> BigInteger.valueOf(Math.abs(rnd.nextLong())))
            .collect(Collectors.toList());
    List<BigInteger> vs =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> BigInteger.valueOf(Math.abs(rnd.nextLong())))
            .collect(Collectors.toList());
    List<BigInteger> ws =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> secrets.get(i).multiply(rs.get(i)))
            .collect(Collectors.toList());
    List<BigInteger> us =
        IntStream.range(0, numberOfValues)
            .mapToObj(i -> vs.get(i).multiply(rs.get(i)))
            .collect(Collectors.toList());
    ws.set(numberOfValues - 1, ws.get(numberOfValues - 1).subtract(BigInteger.TEN));
    SecretVerificationException ove =
        assertThrows(
            SecretVerificationException.class,
            () -> secretShareUtil.verifySecrets(secrets, rs, us, vs, ws));
    assertThat(ove.getMessage(), CoreMatchers.startsWith("Verification of secret has failed"));
  }
}
