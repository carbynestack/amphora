/*https://github.com/carbynestack/amphora
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.calculation;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static io.carbynestack.castor.common.entities.TupleType.MULTIPLICATION_TRIPLE_GFP;
import static io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils.WORD_WIDTH;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import io.carbynestack.amphora.client.DefaultAmphoraInterVcpClient;
import io.carbynestack.amphora.common.FactorPair;
import io.carbynestack.amphora.common.MultiplicationExchangeObject;
import io.carbynestack.amphora.common.OutputDeliveryObject;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.config.AmphoraServiceProperties;
import io.carbynestack.amphora.service.persistence.cache.InterimValueCachingService;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.common.entities.Field;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.MultiplicationTriple;
import io.carbynestack.castor.common.entities.TupleList;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import io.vavr.concurrent.Future;
import io.vavr.control.Try;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service used to generate {@link OutputDeliveryObject}s based on {@link SecretShare}s.
 *
 * <p>See {@link OutputDeliveryObject} for further details.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class OutputDeliveryService {
  private final AmphoraServiceProperties amphoraProperties;
  private final CastorIntraVcpClient castorClient;
  private final MpSpdzIntegrationUtils spdzUtil;
  private final DefaultAmphoraInterVcpClient interVCClient;
  private final InterimValueCachingService interimValueCachingService;

  /**
   * Computes an {@link OutputDeliveryObject} based on the given {@link SecretShare}.
   *
   * @param secretShare the {@link SecretShare} as input to the computation.
   * @param requestId a unique request id to align multiplications across all parties of the CS MPC
   *     cluster.
   * @return the computed {@link OutputDeliveryObject}
   * @throws AmphoraServiceException if the service failed to retrieve the required tuples for
   *     computation
   */
  @Transactional
  public OutputDeliveryObject computeOutputDeliveryObject(SecretShare secretShare, UUID requestId) {
    byte[] shareData = secretShare.getData();
    int secretSize = shareData.length / MpSpdzIntegrationUtils.SHARE_WIDTH;
    TupleList<InputMask<Field.Gfp>, Field.Gfp> inputMaskShares =
        Try.of(() -> castorClient.downloadTupleShares(requestId, INPUT_MASK_GFP, secretSize * 2L))
            .getOrElseThrow(
                e ->
                    new AmphoraServiceException(
                        "Failed to retrieve the required Tuples form Castor", e));
    ByteBuffer secretShares = ByteBuffer.allocate(secretSize * WORD_WIDTH);
    ByteBuffer rShares = ByteBuffer.allocate(secretSize * WORD_WIDTH);
    ByteBuffer wShares = ByteBuffer.allocate(secretSize * WORD_WIDTH);
    ByteBuffer vShares = ByteBuffer.allocate(secretSize * WORD_WIDTH);
    ByteBuffer uShares = ByteBuffer.allocate(secretSize * WORD_WIDTH);

    /*
     * each word/share requires two secret multiplications
     * <w> = <y * r>                    <u> = <v * r>
     * where <y> is the secret share, <r> is input mask 1 and <v> is input mask 2
     * <w> = <secret * inputMask_1>     <u> = <inputMask_2 * inputMask_1>
     */

    List<FactorPair> factorPairs =
        IntStream.range(0, secretSize)
            .mapToObj(
                i -> {
                  byte[] secret =
                      ArrayUtils.subarray(
                          shareData,
                          i * MpSpdzIntegrationUtils.SHARE_WIDTH,
                          i * MpSpdzIntegrationUtils.SHARE_WIDTH + WORD_WIDTH);
                  byte[] mask1 = inputMaskShares.get(i * 2).getShare(0).getValue();
                  byte[] mask2 = inputMaskShares.get((i * 2) + 1).getShare(0).getValue();
                  BigInteger secretAsBI = spdzUtil.fromGfp(secret);
                  BigInteger mask1AsBI = spdzUtil.fromGfp(mask1);
                  BigInteger mask2AsBI = spdzUtil.fromGfp(mask2);
                  secretShares.put(secret);
                  rShares.put(mask1);
                  vShares.put(mask2);
                  return Arrays.asList(
                      FactorPair.of(secretAsBI, mask1AsBI), FactorPair.of(mask2AsBI, mask1AsBI));
                })
            .flatMap(List::stream)
            .collect(Collectors.toList());
    UUID operationId =
        UUID.nameUUIDFromBytes(String.format("%s_%s", requestId, factorPairs.size()).getBytes());
    log.debug(
        "Using operationId #{} to perform multiplications on Amphora get request with id #{}",
        operationId,
        requestId);
    List<BigInteger> products = multiplyShares(operationId, factorPairs);
    IntStream.range(0, secretSize)
        .forEach(
            i -> {
              wShares.put(spdzUtil.toGfp(products.get(i * 2)));
              uShares.put(spdzUtil.toGfp(products.get((i * 2) + 1)));
            });

    return OutputDeliveryObject.builder()
        .secretId(secretShare.getSecretId())
        .secretShares(secretShares.array())
        .rShares(rShares.array())
        .vShares(vShares.array())
        .wShares(wShares.array())
        .uShares(uShares.array())
        .tags(secretShare.getTags())
        .build();
  }

  /**
   * Performs a multiplication on secret shares according to the SPDZ protocol for each pair of
   * factors.
   *
   * @param operationId ID of the multiplication operation used to match the exchanged values across
   *     VCPs
   * @param factorPairs Pair of factors that should be multiplied
   * @return List of the products as result of the multiplications.
   * @throws AmphoraServiceException in case:
   *     <ul>
   *       <li>The Service was unable to fetch the required Tuples from Castor
   *     </ul>
   */
  List<BigInteger> multiplyShares(UUID operationId, List<FactorPair> factorPairs) {
    TupleList<MultiplicationTriple<Field.Gfp>, Field.Gfp> tripleShares =
        Try.of(
                () ->
                    castorClient.downloadTupleShares(
                        operationId, MULTIPLICATION_TRIPLE_GFP, factorPairs.size()))
            .getOrElseThrow(
                e ->
                    new AmphoraServiceException(
                        "Failed to retrieve the required Tuples form Castor", e));
    List<FactorPair> diffShares =
        IntStream.range(0, factorPairs.size())
            .mapToObj(
                i ->
                    FactorPair.of(
                        factorPairs
                            .get(i)
                            .getA()
                            .subtract(spdzUtil.fromGfp(tripleShares.get(i).getShare(0).getValue())),
                        factorPairs
                            .get(i)
                            .getB()
                            .subtract(
                                spdzUtil.fromGfp(tripleShares.get(i).getShare(1).getValue()))))
            .collect(Collectors.toList());
    MultiplicationExchangeObject exchangeObject =
        new MultiplicationExchangeObject(operationId, amphoraProperties.getPlayerId(), diffShares);
    interimValueCachingService.putInterimValues(exchangeObject);
    List<FactorPair> openedDiffs =
        Try.run(() -> interVCClient.open(exchangeObject))
            .onFailure(
                f ->
                    log.error(
                        String.format(
                            "Failed to distribute own shares for operation #%s", operationId),
                        f))
            .map(ignored -> recombineDiffs(operationId))
            .onFailure(
                f ->
                    log.error(
                        String.format("Failed to recombine shares of operation #%s", operationId),
                        f))
            .getOrElseThrow(
                throwable ->
                    new AmphoraServiceException(
                        String.format("Failed to open values for operation #%s", operationId),
                        throwable));
    return IntStream.range(0, factorPairs.size())
        .mapToObj(
            i ->
                multiplySharedSecrets(
                    tripleShares.get(i), openedDiffs.get(i).getA(), openedDiffs.get(i).getB()))
        .collect(Collectors.toList());
  }

  private List<FactorPair> recombineDiffs(UUID operationId) {
    Retryer<List<FactorPair>> interimValueRetryer =
        RetryerBuilder.<List<FactorPair>>newBuilder()
            .retryIfException()
            .retryIfRuntimeException()
            .withStopStrategy(
                StopStrategies.stopAfterDelay(
                    amphoraProperties.getOpeningTimeout(), TimeUnit.MILLISECONDS))
            .build();
    List<Future<List<FactorPair>>> futures =
        IntStream.rangeClosed(0, amphoraProperties.getVcPartners().size())
            .parallel()
            .mapToObj(
                i ->
                    Future.of(
                        () ->
                            interimValueRetryer.call(
                                () ->
                                    interimValueCachingService
                                        .getInterimValuesAndEvict(operationId, i)
                                        .get())))
            .collect(Collectors.toList());
    return Future.reduce(
            futures,
            (diffListA, diffListB) ->
                IntStream.range(0, diffListA.size())
                    .mapToObj(
                        i ->
                            FactorPair.of(
                                diffListA
                                    .get(i)
                                    .getA()
                                    .add(diffListB.get(i).getA())
                                    .mod(spdzUtil.getPrime()),
                                diffListA
                                    .get(i)
                                    .getB()
                                    .add(diffListB.get(i).getB())
                                    .mod(spdzUtil.getPrime())))
                    .collect(Collectors.toList()))
        .get();
  }

  private BigInteger multiplySharedSecrets(
      MultiplicationTriple<Field.Gfp> triple, BigInteger diff1, BigInteger diff2) {
    BigInteger share =
        spdzUtil
            .fromGfp(triple.getShare(2).getValue())
            .add(diff1.multiply(spdzUtil.fromGfp(triple.getShare(1).getValue())))
            .add(diff2.multiply(spdzUtil.fromGfp(triple.getShare(0).getValue())))
            .mod(spdzUtil.getPrime());
    if (amphoraProperties.getPlayerId() == 0) {
      share = share.add(diff1.multiply(diff2)).mod(spdzUtil.getPrime());
    }
    return share;
  }
}
