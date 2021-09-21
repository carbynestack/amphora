/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.google.common.collect.Lists;
import io.carbynestack.amphora.common.AmphoraServiceUri;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DefaultAmphoraClientBuilder.class)
public class DefaultAmphoraClientBuilderTest {
  @SneakyThrows
  @Test
  public void givenEndpointsNotDefined_whenBuildingClient_thenThrowException() {
    DefaultAmphoraClientBuilder builder = DefaultAmphoraClient.builder();
    builder.r(BigInteger.ZERO).rInv(BigInteger.ONE).prime(BigInteger.TEN);
    IllegalArgumentException iae =
        assertThrows(IllegalArgumentException.class, () -> builder.build());
    assertEquals("At least one amphora service uri has to be provided.", iae.getMessage());
  }

  @SneakyThrows
  @Test
  public void givenPrimeOmitted_whenBuildingClient_thenThrowNullPointerException() {
    DefaultAmphoraClientBuilder builder = DefaultAmphoraClient.builder();
    builder
        .r(BigInteger.ZERO)
        .rInv(BigInteger.ONE)
        .addEndpoint(new AmphoraServiceUri("https://amphora.carbynestack.io:80"));
    NullPointerException npe = assertThrows(NullPointerException.class, () -> builder.build());
    assertEquals("Prime must not be null", npe.getMessage());
  }

  @SneakyThrows
  @Test
  public void givenRInvOmitted_whenBuildingClient_thenThrowNullPointerException() {
    DefaultAmphoraClientBuilder builder = DefaultAmphoraClient.builder();
    builder
        .prime(BigInteger.TEN)
        .r(BigInteger.ZERO)
        .addEndpoint(new AmphoraServiceUri("https://amphora.carbynestack.io:80"));
    NullPointerException npe = assertThrows(NullPointerException.class, () -> builder.build());
    assertEquals(
        "Multiplicative inverse for the auxiliary modulus R must not be null", npe.getMessage());
  }

  @SneakyThrows
  @Test
  public void givenROmitted_whenBuildingClient_thenThrowNullPointerException() {
    DefaultAmphoraClientBuilder builder = DefaultAmphoraClient.builder();
    builder
        .prime(BigInteger.TEN)
        .rInv(BigInteger.ONE)
        .addEndpoint(new AmphoraServiceUri("https://amphora.carbynestack.io:80"));
    NullPointerException npe = assertThrows(NullPointerException.class, () -> builder.build());
    assertEquals("Auxiliary modulus R must not be null", npe.getMessage());
  }

  @SneakyThrows
  @Test
  public void givenValidConfiguration_whenBuildingClient_thenCallConstructorWithBuilder() {
    ArgumentCaptor<DefaultAmphoraClientBuilder> builderArgumentCaptor =
        ArgumentCaptor.forClass(DefaultAmphoraClientBuilder.class);

    List<AmphoraServiceUri> endpointList =
        Arrays.asList(
            new AmphoraServiceUri("https://amphora.carbynestack.io:80"),
            new AmphoraServiceUri("https://amphora.carbynestack.io:180"));
    AmphoraServiceUri singleEndpoint = new AmphoraServiceUri("https://amphora.carbynestack.io:280");
    List<File> trustedCertificateList =
        Arrays.asList(
            Files.createTempFile(DefaultAmphoraClientTest.class.getSimpleName(), null).toFile(),
            Files.createTempFile(DefaultAmphoraClientTest.class.getSimpleName(), null).toFile());
    File singleTrustedCertificate =
        Files.createTempFile(DefaultAmphoraClientTest.class.getSimpleName(), null).toFile();
    BigInteger prime = BigInteger.ZERO;
    BigInteger r = BigInteger.ONE;
    BigInteger rInv = BigInteger.TEN;
    BearerTokenProvider<AmphoraServiceUri> bearerTokenProvider = o -> null;

    try (MockedConstruction<DefaultAmphoraClient> ignored =
        mockConstruction(DefaultAmphoraClient.class)) {
      whenNew(DefaultAmphoraClient.class).withAnyArguments().thenReturn(null);
      DefaultAmphoraClient.builder()
          .prime(prime)
          .r(r)
          .rInv(rInv)
          .endpoints(endpointList)
          .addEndpoint(singleEndpoint)
          .trustedCertificates(trustedCertificateList)
          .addTrustedCertificate(singleTrustedCertificate)
          .bearerTokenProvider(bearerTokenProvider)
          .withoutSslCertificateValidation()
          .build();
      PowerMockito.verifyNew(DefaultAmphoraClient.class, times(1))
          .withArguments(builderArgumentCaptor.capture());
    }
    DefaultAmphoraClientBuilder actualBuilder = builderArgumentCaptor.getValue();
    assertEquals(prime, actualBuilder.prime());
    assertEquals(r, actualBuilder.r());
    assertEquals(rInv, actualBuilder.rInv());
    assertEquals(bearerTokenProvider, actualBuilder.bearerTokenProvider());
    assertEquals(3, actualBuilder.trustedCertificates().size());
    assertEquals(3, actualBuilder.serviceUris().size());
    assertTrue(actualBuilder.noSslValidation());
    assertThat(
        (ArrayList<File>) actualBuilder.trustedCertificates(),
        hasItems(
            Lists.asList(singleTrustedCertificate, trustedCertificateList.toArray(new File[0]))
                .toArray(new File[0])));
    assertThat(
        actualBuilder.serviceUris(),
        hasItems(
            Lists.asList(singleEndpoint, endpointList.toArray(new AmphoraServiceUri[0]))
                .toArray(new AmphoraServiceUri[0])));
  }
}
