/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.calculation;

import static io.carbynestack.castor.common.entities.TupleType.INPUT_MASK_GFP;
import static io.carbynestack.castor.common.entities.TupleType.MULTIPLICATION_TRIPLE_GFP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import io.carbynestack.amphora.client.DefaultAmphoraInterVcpClient;
import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.config.AmphoraServiceProperties;
import io.carbynestack.amphora.service.config.CastorClientProperties;
import io.carbynestack.amphora.service.persistence.cache.InterimValueCachingService;
import io.carbynestack.castor.client.download.CastorIntraVcpClient;
import io.carbynestack.castor.common.entities.*;
import io.carbynestack.castor.common.exceptions.CastorClientException;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import io.vavr.control.Option;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class OutputDeliveryServiceTest {
  private final MpSpdzIntegrationUtils spdzUtil =
      MpSpdzIntegrationUtils.of(
          new BigInteger("198766463529478683931867765928436695041"),
          new BigInteger("141515903391459779531506841503331516415"),
          new BigInteger("133854242216446749056083838363708373830"));
  private final int testPlayerId = 0;
  private final List<AmphoraServiceUri> testVcPartners =
      Collections.singletonList(new AmphoraServiceUri("https://vcpartner.carbynestack.io/amphora"));
  private final int testVcPartnerPlayerId = 1;
  private final int testSecretShareDataSize = 2;
  private final List<BigInteger> testSecretShareSecretValues =
      Arrays.asList(BigInteger.valueOf(90), BigInteger.valueOf(142));
  private final byte[] testSecretShareData =
      bigIntegerListToGfpByteArray(
          IntStream.range(0, testSecretShareDataSize)
              // BigInteger.ZERO as MAC value by default since not of interest
              .mapToObj(i -> new BigInteger[] {testSecretShareSecretValues.get(i), BigInteger.ZERO})
              .flatMap(Arrays::stream)
              .collect(Collectors.toList()));
  private final List<BigInteger> testInputMaskValues =
      Arrays.asList(
          BigInteger.valueOf(87),
          BigInteger.valueOf(111),
          BigInteger.valueOf(412),
          BigInteger.valueOf(313));
  private final TupleList<InputMask<Field.Gfp>, Field.Gfp> testInputMasks =
      ((Supplier<TupleList<InputMask<Field.Gfp>, Field.Gfp>>)
              () -> {
                ByteBuffer inputMasksBb =
                    ByteBuffer.wrap(
                        bigIntegerListToGfpByteArray(
                            IntStream.range(0, testSecretShareDataSize * 2)
                                // BigInteger.ZERO as MAC value by default since not of interest
                                .mapToObj(
                                    i ->
                                        new BigInteger[] {
                                          testInputMaskValues.get(i), BigInteger.ZERO
                                        })
                                .flatMap(Arrays::stream)
                                .collect(Collectors.toList())));
                try {
                  return (TupleList<InputMask<Field.Gfp>, Field.Gfp>)
                      TupleList.fromStream(
                          INPUT_MASK_GFP.getTupleCls(),
                          ShareFamily.COWGEAR.getFamilyName(),
                          INPUT_MASK_GFP.getField(),
                          new ByteBufferBackedInputStream(inputMasksBb),
                          inputMasksBb.limit());
                } catch (IOException e) {
                  throw new RuntimeException("Setting up tests failed.", e);
                }
              })
          .get();
  private final List<FactorPair> expectedPrivateMultiplicationPairs =
      IntStream.range(0, testSecretShareDataSize)
          .mapToObj(
              i ->
                  new FactorPair[] {
                    FactorPair.of(
                        testSecretShareSecretValues.get(i), testInputMaskValues.get(i * 2)),
                    FactorPair.of(
                        testInputMaskValues.get((i * 2) + 1), testInputMaskValues.get(i * 2))
                  })
          .flatMap(Arrays::stream)
          .collect(Collectors.toList());
  private final List<List<BigInteger>> testTripleShareValues =
      Arrays.asList(
          Arrays.asList(BigInteger.valueOf(80), BigInteger.valueOf(62), BigInteger.valueOf(3719)),
          Arrays.asList(BigInteger.valueOf(72), BigInteger.valueOf(63), BigInteger.valueOf(32521)),
          Arrays.asList(
              BigInteger.valueOf(141), BigInteger.valueOf(264), BigInteger.valueOf(56212)),
          Arrays.asList(BigInteger.valueOf(19), BigInteger.valueOf(35), BigInteger.valueOf(612)));
  private final TupleList<MultiplicationTriple<Field.Gfp>, Field.Gfp> testTriples =
      ((Supplier<TupleList<MultiplicationTriple<Field.Gfp>, Field.Gfp>>)
              () -> {
                // BigInteger.ZERO as MAC value by default since not of interest
                ByteBuffer triplesBb =
                    ByteBuffer.wrap(
                        bigIntegerListToGfpByteArray(
                            testTripleShareValues.stream()
                                .map(
                                    testTripleShareValue ->
                                        new BigInteger[] {
                                          testTripleShareValue.get(0), BigInteger.ZERO,
                                          testTripleShareValue.get(1), BigInteger.ZERO,
                                          testTripleShareValue.get(2), BigInteger.ZERO
                                        })
                                .flatMap(Arrays::stream)
                                .collect(Collectors.toList())));
                try {
                  return (TupleList<MultiplicationTriple<Field.Gfp>, Field.Gfp>)
                      TupleList.fromStream(
                          MULTIPLICATION_TRIPLE_GFP.getTupleCls(),
                          ShareFamily.COWGEAR.getFamilyName(),
                          MULTIPLICATION_TRIPLE_GFP.getField(),
                          new ByteBufferBackedInputStream(triplesBb),
                          triplesBb.limit());
                } catch (IOException e) {
                  throw new RuntimeException("Setting up tests failed.", e);
                }
              })
          .get();
  private final List<FactorPair> expectedPrivateOpeningInput =
      Arrays.asList(
          FactorPair.of(BigInteger.valueOf(10), BigInteger.valueOf(25)),
          FactorPair.of(BigInteger.valueOf(39), BigInteger.valueOf(24)),
          FactorPair.of(BigInteger.valueOf(1), BigInteger.valueOf(148)),
          FactorPair.of(BigInteger.valueOf(294), BigInteger.valueOf(377)));
  private final List<FactorPair> expectedPartnerOpeningInput =
      Arrays.asList(
          FactorPair.of(BigInteger.valueOf(4), BigInteger.valueOf(63)),
          FactorPair.of(BigInteger.valueOf(175), BigInteger.valueOf(136)),
          FactorPair.of(BigInteger.valueOf(5), BigInteger.valueOf(106)),
          FactorPair.of(BigInteger.valueOf(2), BigInteger.valueOf(27)));
  private final List<BigInteger> expectedPrivateProductShares =
      Arrays.asList(
          BigInteger.valueOf(12859), BigInteger.valueOf(91763),
          BigInteger.valueOf(95134), BigInteger.valueOf(138232));
  private final UUID expectedRequestId = UUID.fromString("70297fd4-d412-4dbb-af05-6818fe0e687a");
  private final UUID expectedOperationId = UUID.fromString("8065e700-9f48-36ba-ae8c-f881b28a28ef");
  public final MultiplicationExchangeObject expectedExchangeObject =
      new MultiplicationExchangeObject(
          expectedOperationId, testPlayerId, expectedPrivateOpeningInput);
  private final SecretShare SECRET_SHARE =
      SecretShare.builder()
          .data(testSecretShareData)
          .secretId(UUID.fromString("5decd680-bdec-4426-bcc6-376ef232e474"))
          .tags(Collections.emptyList())
          .build();

  @Mock private AmphoraServiceProperties amphoraServicePropertiesMock;
  @Mock private CastorIntraVcpClient castorIntraVcpClientMock;
  @Mock private CastorClientProperties castorClientPropertiesMock;
  @Mock private DefaultAmphoraInterVcpClient interVCClientMock;
  @Mock private InterimValueCachingService interimValueCachingServiceMock;

  private OutputDeliveryService outputDeliveryService;

  private byte[] bigIntegerListToGfpByteArray(List<BigInteger> input) {
    return ArrayUtils.toPrimitive(
        input.stream()
            .map(spdzUtil::toGfp)
            .map(ArrayUtils::toObject)
            .flatMap(Arrays::stream)
            .toArray(Byte[]::new));
  }

  @BeforeEach
  public void setup() {
    outputDeliveryService =
        new OutputDeliveryService(
            amphoraServicePropertiesMock,
            castorIntraVcpClientMock,
            spdzUtil,
            interVCClientMock,
            interimValueCachingServiceMock);
  }

  @Test
  @SneakyThrows
  void given_FetchingTriplesFails_whenMultiplyingShares_thenThrowException() {
    when(castorIntraVcpClientMock.downloadTupleShares(
            expectedOperationId,
            MULTIPLICATION_TRIPLE_GFP,
            expectedPrivateMultiplicationPairs.size(),
            TupleFamily.COWGEAR))
        .thenThrow(new CastorClientException("No tuples"));
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () ->
                outputDeliveryService.multiplyShares(
                    expectedOperationId, expectedPrivateMultiplicationPairs, ShareFamily.COWGEAR));
    assertEquals("Failed to retrieve the required Tuples form Castor", ase.getMessage());
  }

  @Test
  @SneakyThrows
  void givenDistributingValuesFails_whenMultiplyingShares_thenThrowException() {
    when(castorIntraVcpClientMock.downloadTupleShares(
            expectedOperationId,
            MULTIPLICATION_TRIPLE_GFP,
            expectedPrivateMultiplicationPairs.size(),
            TupleFamily.COWGEAR))
        .thenReturn(testTriples);
    doThrow(new AmphoraClientException("Failed"))
        .when(interVCClientMock)
        .open(expectedExchangeObject);
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () ->
                outputDeliveryService.multiplyShares(
                    expectedOperationId, expectedPrivateMultiplicationPairs, ShareFamily.COWGEAR));
    assertEquals(
        ase.getMessage(),
        String.format("Failed to open values for operation #%s", expectedOperationId));
    verify(interimValueCachingServiceMock, times(1)).putInterimValues(expectedExchangeObject);
    verify(interVCClientMock, times(1)).open(expectedExchangeObject);
  }

  @Test
  @SneakyThrows
  void givenReceivingAllOpenedSharesFails_whenMultiplyingShares_thenThrowException() {
    when(amphoraServicePropertiesMock.getPlayerId()).thenReturn(testPlayerId);
    when(amphoraServicePropertiesMock.getVcPartners()).thenReturn(testVcPartners);
    when(amphoraServicePropertiesMock.getOpeningTimeout()).thenReturn(10);
    when(castorIntraVcpClientMock.downloadTupleShares(
            expectedOperationId,
            MULTIPLICATION_TRIPLE_GFP,
            expectedPrivateMultiplicationPairs.size(),
            TupleFamily.COWGEAR))
        .thenReturn(testTriples);
    when(interimValueCachingServiceMock.getInterimValuesAndEvict(expectedOperationId, testPlayerId))
        .thenReturn(Option.of(expectedPrivateOpeningInput));
    when(interimValueCachingServiceMock.getInterimValuesAndEvict(
            expectedOperationId, testVcPartnerPlayerId))
        .thenReturn(Option.none());
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () ->
                outputDeliveryService.multiplyShares(
                    expectedOperationId, expectedPrivateMultiplicationPairs, ShareFamily.COWGEAR));
    assertEquals(
        ase.getMessage(),
        String.format("Failed to open values for operation #%s", expectedOperationId));
    verify(interimValueCachingServiceMock, times(1)).putInterimValues(expectedExchangeObject);
    verify(interVCClientMock, times(1)).open(expectedExchangeObject);
    verify(interimValueCachingServiceMock, times(1))
        .getInterimValuesAndEvict(expectedOperationId, testPlayerId);
    verify(interimValueCachingServiceMock, atLeastOnce())
        .getInterimValuesAndEvict(expectedOperationId, testVcPartnerPlayerId);
  }

  @Test
  @SneakyThrows
  void givenSuccessfulRequest_whenMultiplyingShares_thenReturnExpectedContent() {
    when(amphoraServicePropertiesMock.getPlayerId()).thenReturn(testPlayerId);
    when(amphoraServicePropertiesMock.getVcPartners()).thenReturn(testVcPartners);
    when(amphoraServicePropertiesMock.getOpeningTimeout()).thenReturn(10);
    when(castorIntraVcpClientMock.downloadTupleShares(
            expectedOperationId,
            MULTIPLICATION_TRIPLE_GFP,
            expectedPrivateMultiplicationPairs.size(),
            TupleFamily.COWGEAR))
        .thenReturn(testTriples);
    when(interimValueCachingServiceMock.getInterimValuesAndEvict(expectedOperationId, testPlayerId))
        .thenReturn(Option.of(expectedPrivateOpeningInput));
    when(interimValueCachingServiceMock.getInterimValuesAndEvict(
            expectedOperationId, testVcPartnerPlayerId))
        .thenReturn(Option.of(expectedPartnerOpeningInput));
    assertThat(
        outputDeliveryService.multiplyShares(
            expectedOperationId, expectedPrivateMultiplicationPairs, ShareFamily.COWGEAR),
        Matchers.containsInAnyOrder(expectedPrivateProductShares.toArray()));
    verify(interimValueCachingServiceMock, times(1)).putInterimValues(expectedExchangeObject);
    verify(interVCClientMock, times(1)).open(expectedExchangeObject);
    verify(interimValueCachingServiceMock, times(1))
        .getInterimValuesAndEvict(expectedOperationId, testPlayerId);
    verify(interimValueCachingServiceMock, times(1))
        .getInterimValuesAndEvict(expectedOperationId, testVcPartnerPlayerId);
  }

  @SneakyThrows
  @Test
  void
      givenCastorClientDoesNotReturnInputMasks_whenComputingOutputDeliveryObject_thenThrowException() {
    when(castorIntraVcpClientMock.downloadTupleShares(
            expectedRequestId, INPUT_MASK_GFP, testSecretShareDataSize * 2, TupleFamily.COWGEAR))
        .thenThrow(new CastorClientException("Failed fetching input masks"));
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class,
            () ->
                outputDeliveryService.computeOutputDeliveryObject(SECRET_SHARE, expectedRequestId));
    assertEquals("Failed to retrieve the required Tuples form Castor", ase.getMessage());
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenComputingOutputDeliveryObject_thenReturnExpectedContent() {
    final OutputDeliveryObject expectedOutputDeliveryObject =
        OutputDeliveryObject.builder()
            .secretShares(bigIntegerListToGfpByteArray(testSecretShareSecretValues))
            .rShares(
                bigIntegerListToGfpByteArray(
                    IntStream.range(0, testInputMaskValues.size())
                        .filter(i -> i % 2 == 0)
                        .mapToObj(testInputMaskValues::get)
                        .collect(Collectors.toList())))
            .vShares(
                bigIntegerListToGfpByteArray(
                    IntStream.range(0, testInputMaskValues.size())
                        .filter(i -> i % 2 == 1)
                        .mapToObj(testInputMaskValues::get)
                        .collect(Collectors.toList())))
            .wShares(
                bigIntegerListToGfpByteArray(
                    IntStream.range(0, expectedPrivateProductShares.size())
                        .filter(i -> i % 2 == 0)
                        .mapToObj(expectedPrivateProductShares::get)
                        .collect(Collectors.toList())))
            .uShares(
                bigIntegerListToGfpByteArray(
                    IntStream.range(0, expectedPrivateProductShares.size())
                        .filter(i -> i % 2 == 1)
                        .mapToObj(expectedPrivateProductShares::get)
                        .collect(Collectors.toList())))
            .build();

    when(amphoraServicePropertiesMock.getPlayerId()).thenReturn(testPlayerId);
    when(amphoraServicePropertiesMock.getVcPartners()).thenReturn(testVcPartners);
    when(amphoraServicePropertiesMock.getOpeningTimeout()).thenReturn(10);
    when(castorIntraVcpClientMock.downloadTupleShares(
            expectedRequestId, INPUT_MASK_GFP, testSecretShareDataSize * 2, TupleFamily.COWGEAR))
        .thenReturn(testInputMasks);
    when(castorIntraVcpClientMock.downloadTupleShares(
            expectedOperationId,
            MULTIPLICATION_TRIPLE_GFP,
            expectedPrivateMultiplicationPairs.size(),
            TupleFamily.COWGEAR))
        .thenReturn(testTriples);
    when(interimValueCachingServiceMock.getInterimValuesAndEvict(expectedOperationId, testPlayerId))
        .thenReturn(Option.of(expectedPrivateOpeningInput));
    when(interimValueCachingServiceMock.getInterimValuesAndEvict(
            expectedOperationId, testVcPartnerPlayerId))
        .thenReturn(Option.of(expectedPartnerOpeningInput));

    assertEquals(
        expectedOutputDeliveryObject,
        outputDeliveryService.computeOutputDeliveryObject(SECRET_SHARE, expectedRequestId));
    verify(interimValueCachingServiceMock, times(1)).putInterimValues(expectedExchangeObject);
    verify(interVCClientMock, times(1)).open(expectedExchangeObject);
  }
}
