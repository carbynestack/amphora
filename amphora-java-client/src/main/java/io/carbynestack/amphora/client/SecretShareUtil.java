/*
 * Copyright (c) 2021-2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.client;

import static io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils.WORD_WIDTH;

import io.carbynestack.amphora.common.MaskedInputData;
import io.carbynestack.amphora.common.exceptions.IntegrityVerificationException;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/** Utility class for SPDZ related operations on shared or secret data. */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class SecretShareUtil {
  @Getter @NonNull private final BigInteger prime;
  @Getter @NonNull private final BigInteger r;
  @Getter @NonNull private final BigInteger rInv;
  @NonNull private final MpSpdzIntegrationUtils spdzUtil;

  /**
   * Creates a new {@link SecretShareUtil} with the given configuration
   *
   * @param prime the Prime as used by the MPC backend.
   * @param r the auxiliary modulus R as used by the MPC backend.
   * @param rInv the multiplicative inverse for the auxiliary modulus R as used by the MPC backend.
   * @return The new {@link SecretShareUtil}
   * @throws NullPointerException if any of the parameters is <i>null</i>
   */
  static SecretShareUtil of(
      @NonNull BigInteger prime, @NonNull BigInteger r, @NonNull BigInteger rInv) {
    return new SecretShareUtil(prime, r, rInv, MpSpdzIntegrationUtils.of(prime, r, rInv));
  }

  Collector<byte[], BigInteger[], BigInteger> summingGfpAsBigInteger() {
    return Collector.of(
        () -> new BigInteger[] {BigInteger.ZERO},
        (a, v) -> a[0] = a[0].add(spdzUtil.fromGfp(v)),
        (a, b) -> {
          a[0] = a[0].add(b[0]);
          return a;
        },
        t -> t[0].mod(prime),
        Collector.Characteristics.UNORDERED);
  }

  MaskedInputData maskInput(BigInteger secret, BigInteger inputMask) {
    BigInteger maskedInput = secret.subtract(inputMask).mod(prime);
    return MaskedInputData.of(spdzUtil.toGfp(maskedInput));
  }

  List<BigInteger> recombineObject(List<byte[]> shares) {
    if (shares.isEmpty()) {
      return new ArrayList<>();
    }
    return zippedMapToOrderedList(
        IntStream.range(0, shares.get(0).length / WORD_WIDTH)
            .boxed()
            .parallel()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    i ->
                        IntStream.range(0, shares.size())
                            .boxed()
                            .parallel()
                            .map(
                                j ->
                                    Arrays.copyOfRange(
                                        shares.get(j), i * WORD_WIDTH, (i + 1) * WORD_WIDTH))
                            .collect(this.summingGfpAsBigInteger()))));
  }

  /**
   * Verifies the received secret(s) based on the output delivery protocol described by Ivan
   * Damgård, Kasper Damgård, Kurt Nielsen, Peter Sebastian Nordholt, Tomas Toft: <br>
   * Confidential Benchmarking based on Multiparty Computation; IACR Cryptology ePrint Archive 2015:
   * 1006 (2015) <br>
   * <a href="https://eprint.iacr.org/2015/1006">Confidential Benchmarking based on Multiparty
   * Computation</a>
   *
   * @throws IntegrityVerificationException If the verification fails
   */
  void verifySecrets(
      List<BigInteger> secrets,
      List<BigInteger> rs,
      List<BigInteger> us,
      List<BigInteger> vs,
      List<BigInteger> ws) {
    IntStream.range(0, secrets.size())
        .parallel()
        .forEach(
            i -> {
              BigInteger actualW = secrets.get(i).multiply(rs.get(i)).mod(prime);
              BigInteger actualU = vs.get(i).multiply(rs.get(i)).mod(prime);
              if (!ws.get(i).equals(actualW) || !us.get(i).equals(actualU)) {
                throw new IntegrityVerificationException(
                    String.format(
                        "Verification of secret has failed:%n"
                            + "\t%s = %s * %s   &&   %s = %s * %s%n"
                            + "\t%s = %s   &&   %s = %s",
                        ws.get(i),
                        secrets.get(i),
                        rs.get(i),
                        us.get(i),
                        vs.get(i),
                        rs.get(i),
                        ws.get(i),
                        actualW,
                        us.get(i),
                        actualU));
              } else if (log.isDebugEnabled()) {
                log.debug(
                    "Secret verified: {} = {} * {}   &&   {} = {} * {}",
                    ws.get(i),
                    secrets.get(i),
                    rs.get(i),
                    us.get(i),
                    vs.get(i),
                    rs.get(i));
              }
            });
  }

  /**
   * Returns the {@link Map#values()} of a Map sorted in natural order based on the entry's {@link
   * Map.Entry#getKey() key}
   *
   * @param in Map which values to be returned in natural order based on the entries' keys
   * @param <T> Type of the {@link Map#values() values} stored in the map
   * @return ordered list of the values
   */
  <T> List<T> zippedMapToOrderedList(@NonNull Map<Integer, T> in) {
    return in.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }
}
