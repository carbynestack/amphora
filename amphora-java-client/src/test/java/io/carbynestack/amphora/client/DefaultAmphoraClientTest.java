/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.client;

import static io.carbynestack.amphora.client.DefaultAmphoraClient.FETCHING_TAG_WITH_KEY_FOR_SECRET_FAILED_EXCEPTION_MSG;
import static io.carbynestack.amphora.client.DefaultAmphoraClient.REQUEST_FOR_ENDPOINT_FAILED_EXCEPTION_MSG;
import static io.carbynestack.amphora.client.TestData.*;
import static io.carbynestack.amphora.common.TagFilterOperator.EQUALS;
import static io.carbynestack.amphora.common.TagFilterOperator.LESS_THAN;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.*;
import static io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils.WORD_WIDTH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.carbynestack.amphora.client.AmphoraCommunicationClient.RequestParameters;
import io.carbynestack.amphora.client.AmphoraCommunicationClient.RequestParametersWithBody;
import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.common.exceptions.AmphoraClientException;
import io.carbynestack.amphora.common.paging.PageRequest;
import io.carbynestack.amphora.common.paging.Sort;
import io.carbynestack.castor.common.entities.*;
import io.carbynestack.httpclient.CsHttpClient;
import io.carbynestack.httpclient.CsHttpClientException;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAmphoraClientTest {
  private final String testUri = "https://dev.bar.com";
  private final String testUri2 = "https://dev.foo.com";
  private final List<AmphoraServiceUri> testServiceUris =
      Arrays.asList(new AmphoraServiceUri(testUri), new AmphoraServiceUri(testUri2));
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
  private final Tag testTag = Tag.builder().key("key1").value("tag1").build();
  private final Random random = new Random(42);
  private final long secret = 42;

  @Mock private AmphoraCommunicationClient<String> amphoraCommunicationClient;

  private DefaultAmphoraClient amphoraClient;

  @Captor private ArgumentCaptor<List<RequestParametersWithBody<Tag>>> requestParamsWithBodyCaptor;

  private MpSpdzIntegrationUtils spdzUtil;

  @Before
  public void setUp() {
    spdzUtil =
        MpSpdzIntegrationUtils.of(
            new BigInteger("198766463529478683931867765928436695041"),
            new BigInteger("141515903391459779531506841503331516415"),
            new BigInteger("133854242216446749056083838363708373830"));

    this.amphoraClient =
        new DefaultAmphoraClient(
            new DefaultAmphoraClientBuilder()
                .prime(spdzUtil.getPrime())
                .r(spdzUtil.getR())
                .rInv(spdzUtil.getRInv())
                .endpoints(testServiceUris),
            amphoraCommunicationClient);
  }

  @SneakyThrows
  @Test
  public void givenBuilderConfiguration_whenCreatingClient_thenInstantiateAccordingly() {
    List<AmphoraServiceUri> expectedServiceUriList =
        Arrays.asList(
            new AmphoraServiceUri("https://testUri:80"),
            new AmphoraServiceUri("https://testUri:180"));
    List<File> expectedTrustedCertificateList =
        Arrays.asList(
            Files.createTempFile(DefaultAmphoraClientTest.class.getSimpleName(), null).toFile(),
            Files.createTempFile(DefaultAmphoraClientTest.class.getSimpleName(), null).toFile());
    BigInteger expectedPrime = BigInteger.ZERO;
    BigInteger expectedR = BigInteger.ONE;
    BigInteger expectedRInv = BigInteger.TEN;
    BearerTokenProvider<AmphoraServiceUri> expectedBearerTokenProvider = o -> null;
    boolean expectedNoSslValidation = true;
    AmphoraCommunicationClient<String> expectedAmphoraCommunicationClient =
        mock(AmphoraCommunicationClient.class);
    SecretShareUtil expectedSecretShareUtil = mock(SecretShareUtil.class);

    try (MockedStatic<AmphoraCommunicationClient> amphoraCommunicationClientMockedStatic =
        mockStatic(AmphoraCommunicationClient.class)) {
      amphoraCommunicationClientMockedStatic
          .when(
              () ->
                  AmphoraCommunicationClient.of(
                      String.class, expectedNoSslValidation, expectedTrustedCertificateList))
          .thenReturn(expectedAmphoraCommunicationClient);
      try (MockedStatic<SecretShareUtil> secretShareUtilMockedStatic =
          mockStatic(SecretShareUtil.class)) {
        secretShareUtilMockedStatic
            .when(() -> SecretShareUtil.of(expectedPrime, expectedR, expectedRInv))
            .thenReturn(expectedSecretShareUtil);
        DefaultAmphoraClientBuilder mockedDefaultAmphoraClientBuilder =
            getMockedDefaultAmphoraClientBuilder(
                expectedServiceUriList,
                expectedTrustedCertificateList,
                expectedPrime,
                expectedR,
                expectedRInv,
                expectedBearerTokenProvider,
                expectedNoSslValidation);

        DefaultAmphoraClient amphoraClient =
            new DefaultAmphoraClient(mockedDefaultAmphoraClientBuilder);

        assertEquals(expectedAmphoraCommunicationClient, amphoraClient.communicationClient);
        assertEquals(expectedSecretShareUtil, amphoraClient.secretShareUtil);
        assertEquals(Option.some(expectedBearerTokenProvider), amphoraClient.bearerTokenProvider);
        assertEquals(expectedServiceUriList, amphoraClient.serviceUris);
      }
    }
  }

  private DefaultAmphoraClientBuilder getMockedDefaultAmphoraClientBuilder(
      List<AmphoraServiceUri> serviceUriList,
      List<File> trustedCertificateList,
      BigInteger prime,
      BigInteger r,
      BigInteger rInv,
      BearerTokenProvider<AmphoraServiceUri> bearerTokenProvider,
      boolean noSslValidation) {
    DefaultAmphoraClientBuilder mockedDefaultAmphoraClientBuilder =
        mock(DefaultAmphoraClientBuilder.class);
    doReturn(prime).when(mockedDefaultAmphoraClientBuilder).prime();
    doReturn(r).when(mockedDefaultAmphoraClientBuilder).r();
    doReturn(rInv).when(mockedDefaultAmphoraClientBuilder).rInv();
    doReturn(noSslValidation).when(mockedDefaultAmphoraClientBuilder).noSslValidation();
    doReturn(trustedCertificateList).when(mockedDefaultAmphoraClientBuilder).trustedCertificates();
    doReturn(serviceUriList).when(mockedDefaultAmphoraClientBuilder).serviceUris();
    doReturn(bearerTokenProvider).when(mockedDefaultAmphoraClientBuilder).bearerTokenProvider();
    return mockedDefaultAmphoraClientBuilder;
  }

  @SneakyThrows
  @Test
  public void givenInvalidCertFile_whenCreatingClient_thenThrowException() {
    CsHttpClientException expectedException = new CsHttpClientException("Expectedly failed");
    DefaultAmphoraClientBuilder mockedDefaultAmphoraClientBuilder =
        mock(DefaultAmphoraClientBuilder.class);
    CsHttpClient.CsHttpClientBuilder<String> mockedSpecsHttpClientBuilder =
        mock(CsHttpClient.CsHttpClientBuilder.class);
    doReturn(mockedSpecsHttpClientBuilder)
        .when(mockedSpecsHttpClientBuilder)
        .withFailureType(any());
    doReturn(mockedSpecsHttpClientBuilder)
        .when(mockedSpecsHttpClientBuilder)
        .withTrustedCertificates(any());
    doReturn(mockedSpecsHttpClientBuilder)
        .when(mockedSpecsHttpClientBuilder)
        .withoutSslValidation(anyBoolean());
    doThrow(expectedException).when(mockedSpecsHttpClientBuilder).build();
    try (MockedStatic<CsHttpClient> specsHttpClientMockedStatic = mockStatic(CsHttpClient.class)) {
      specsHttpClientMockedStatic
          .when(CsHttpClient::builder)
          .thenReturn(mockedSpecsHttpClientBuilder);
      AmphoraClientException ace =
          assertThrows(
              AmphoraClientException.class,
              () -> new DefaultAmphoraClient(mockedDefaultAmphoraClientBuilder));
      assertEquals(expectedException, ace.getCause());
      assertEquals("Failed to create AmphoraCommunicationClient.", ace.getMessage());
    }
  }

  @SneakyThrows
  @Test
  public void givenSecretsOfVariousSize_whenSharingAndRecombining_thenRecoverInitialData() {
    ArgumentCaptor<List<RequestParametersWithBody<MaskedInput>>> requestCaptor =
        ArgumentCaptor.forClass(List.class);
    Map<URI, Try<UUID>> uriResponseMap = getSuccessUriResponseMap();
    for (int i = 0; i < 100; i++) {
      int secretSize = random.nextInt(1000) + 1;
      BigInteger[] secrets =
          random
              .longs(secretSize, 0, Long.MAX_VALUE)
              .mapToObj(BigInteger::valueOf)
              .toArray(BigInteger[]::new);
      Map<URI, TupleList> inputMaskShares = getInputMaskShares(secretSize);
      when(amphoraCommunicationClient.download(anyList(), eq(TupleList.class)))
          .thenReturn(TestUtils.wrap(inputMaskShares));
      when(amphoraCommunicationClient.upload(anyList(), eq(UUID.class))).thenReturn(uriResponseMap);
      UUID storeResult = amphoraClient.createSecret(getObject(secrets));
      assertNotNull(storeResult);
      verify(amphoraCommunicationClient, times(1)).upload(requestCaptor.capture(), any());
      List<MaskedInput> capturedMaskedInputs =
          requestCaptor.getValue().stream()
              .map(RequestParametersWithBody::getBody)
              .collect(Collectors.toList());
      assertEquals(2, capturedMaskedInputs.size());
      MaskedInput maskedInput = capturedMaskedInputs.get(0);
      BigInteger[] recoveredObject = new BigInteger[secretSize];
      for (int j = 0; j < secretSize; j++) {
        BigInteger mask = BigInteger.ZERO;
        int finalJ = j;
        for (BigInteger b :
            inputMaskShares.values().stream()
                .map(list -> (InputMask) list.get(finalJ))
                .map(inputMask -> inputMask.getShare(0))
                .map(Share::getValue)
                .map(spdzUtil::fromGfp)
                .collect(Collectors.toList())) {
          mask = mask.add(b).mod(spdzUtil.getPrime());
        }
        BigInteger maskedInputValue = spdzUtil.fromGfp(maskedInput.getData().get(j).getValue());
        recoveredObject[j] = mask.add(maskedInputValue).mod(spdzUtil.getPrime());
      }
      assertArrayEquals(secrets, recoveredObject);
      Mockito.reset(amphoraCommunicationClient);
    }
  }

  @SneakyThrows
  @Test
  public void givenUploadingMaskedInputFails_whenCreateObject_thenThrowAmphoraClientException() {
    Map<URI, Try<URI>> uriResponseMap = getErrorUriResponseMap();
    when(amphoraCommunicationClient.download(anyList(), eq(TupleList.class)))
        .thenReturn(TestUtils.wrap(getInputMaskShares(1)));
    when(amphoraCommunicationClient.upload(anyList(), eq(URI.class))).thenReturn(uriResponseMap);
    AmphoraClientException actualAce =
        assertThrows(
            AmphoraClientException.class,
            () ->
                amphoraClient.createSecret(
                    getObject(new BigInteger[] {BigInteger.valueOf(secret)})));
    assertThat(
        actualAce.getMessage(),
        CoreMatchers.endsWith(
            String.format(REQUEST_FOR_ENDPOINT_FAILED_EXCEPTION_MSG, testUri, 500)));
  }

  @SneakyThrows
  @Test
  public void
      givenOutPutDeliveryObjectsOfVariousSize_whenDownloadingObject_thenReturnExpectedContent() {
    for (int i = 0; i < 100; i++) {
      int secretSize = random.nextInt(1000) + 1; // make sure it is no 0
      BigInteger[] secrets =
          random
              .longs(secretSize, 0, Long.MAX_VALUE)
              .map(s -> s < 0 ? -s : s)
              .mapToObj(BigInteger::valueOf)
              .toArray(BigInteger[]::new);
      when(amphoraCommunicationClient.download(anyList(), eq(OutputDeliveryObject.class)))
          .thenReturn(TestUtils.wrap(getOutputDeliveryObjectsForSecrets(testSecretId, secrets)));
      Secret result = amphoraClient.getSecret(testSecretId);
      assertEquals(secretSize, result.getData().length);
      assertThat(Arrays.asList(result.getData()), CoreMatchers.hasItems(secrets));
    }
  }

  @SneakyThrows
  @Test
  public void
      givenDownloadDataFromOnePlayerFails_whenDownloadingObject_thenThrowAmphoraClientException() {
    BigInteger[] secrets = new BigInteger[] {BigInteger.valueOf(42), BigInteger.valueOf(24)};
    Map<URI, Try<OutputDeliveryObject>> results =
        TestUtils.wrap(getOutputDeliveryObjectsForSecrets(testSecretId, secrets));
    results.put(
        results.keySet().toArray(new URI[0])[results.size() - 1],
        Try.failure(new Exception("Call failed")));
    when(amphoraCommunicationClient.download(anyList(), eq(OutputDeliveryObject.class)))
        .thenReturn(results);
    AmphoraClientException ace =
        assertThrows(AmphoraClientException.class, () -> amphoraClient.getSecret(testSecretId));
    assertThat(
        ace.getMessage(), CoreMatchers.startsWith("Error(s) occurred while processing responses"));
    assertThat(ace.getMessage(), CoreMatchers.containsString("Call failed"));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjects_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    when(amphoraCommunicationClient.download(any(RequestParameters.class), eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    List<Metadata> secrets = amphoraClient.getSecrets();
    assertThat(secrets, is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithEqualsFilter_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    String key = "key";
    String value = "value";
    ArrayList<TagFilter> filters = new ArrayList<>();
    filters.add(TagFilter.with(key, value, EQUALS));
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + FILTER_PARAMETER
            + "="
            + key
            + "%3A"
            + value;
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    List<Metadata> secrets = amphoraClient.getSecrets(filters);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets, is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithLessThanFilter_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    String key = "key";
    String value = "123";
    ArrayList<TagFilter> filters = new ArrayList<>();
    filters.add(TagFilter.with(key, value, LESS_THAN));
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + FILTER_PARAMETER
            + "="
            + key
            + "%3C"
            + value;
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    List<Metadata> secrets = amphoraClient.getSecrets(filters);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets, is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithTwoFilters_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    String key1 = "key";
    String value1 = "value";
    String key2 = "key_2";
    String value2 = "123";
    ArrayList<TagFilter> filters = new ArrayList<>();
    filters.add(TagFilter.with(key1, value1 + ",", EQUALS));
    filters.add(TagFilter.with(key2, value2, LESS_THAN));
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + FILTER_PARAMETER
            + "="
            + key1
            + "%3A"
            + value1
            + "%252C%2C"
            + key2
            + "%3C"
            + value2;
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    List<Metadata> secrets = amphoraClient.getSecrets(filters);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets, is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithFilterAndEmptyValue_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    String key1 = "key";
    String value1 = "";
    String key2 = "key_2";
    String value2 = "123";
    ArrayList<TagFilter> filters = new ArrayList<>();
    filters.add(TagFilter.with(key1, value1, EQUALS));
    filters.add(TagFilter.with(key2, value2, LESS_THAN));
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + FILTER_PARAMETER
            + "="
            + key1
            + "%3A"
            + value1
            + "%2C"
            + key2
            + "%3C"
            + value2;
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    List<Metadata> secrets = amphoraClient.getSecrets(filters);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets, is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithPagination_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    ArrayList<TagFilter> filters = new ArrayList<>();
    int pageNumber = 3;
    int pageSize = 50;
    PageRequest pageRequest = PageRequest.builder().page(pageNumber).size(pageSize).build();
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + PAGE_NUMBER_PARAMETER
            + "="
            + pageNumber
            + "&"
            + PAGE_SIZE_PARAMETER
            + "="
            + pageSize;
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    MetadataPage secrets = amphoraClient.getSecrets(filters, pageRequest);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets.getContent(), is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithPaginationAndSorting_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    ArrayList<TagFilter> filters = new ArrayList<>();
    int pageNumber = 3;
    int pageSize = 50;
    String property = "key1";
    PageRequest pageRequest =
        PageRequest.builder()
            .page(pageNumber)
            .size(pageSize)
            .sort(Sort.by(property, Sort.Order.ASC))
            .build();
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + PAGE_NUMBER_PARAMETER
            + "="
            + pageNumber
            + "&"
            + PAGE_SIZE_PARAMETER
            + "="
            + pageSize
            + "&"
            + SORT_PROPERTY_PARAMETER
            + "="
            + property
            + "&"
            + SORT_DIRECTION_PARAMETER
            + "="
            + Sort.Order.ASC.name();
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    MetadataPage secrets = amphoraClient.getSecrets(filters, pageRequest);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets.getContent(), is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithSorting_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    String property = "key1";
    Sort.Order direction = Sort.Order.ASC;
    Sort sort = Sort.by(property, direction);
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + SORT_PROPERTY_PARAMETER
            + "="
            + property
            + "&"
            + SORT_DIRECTION_PARAMETER
            + "="
            + direction.name();
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    List<Metadata> secrets = amphoraClient.getSecrets(sort);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets, is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithFilterAndSorting_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    ArrayList<TagFilter> filters = new ArrayList<>();
    String key = "key";
    String value = "123";
    filters.add(TagFilter.with(key, value, LESS_THAN));
    String property = "key1";
    Sort.Order direction = Sort.Order.DESC;
    Sort sortDesc = Sort.by(property, direction);
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + FILTER_PARAMETER
            + "="
            + key
            + "%3C"
            + value
            + "&"
            + SORT_PROPERTY_PARAMETER
            + "="
            + property
            + "&"
            + SORT_DIRECTION_PARAMETER
            + "="
            + direction.name();
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    List<Metadata> secrets = amphoraClient.getSecrets(filters, sortDesc);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets, is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void
      givenSuccessfulRequest_whenListingObjectsWithFiltersAndPaginationAndSorting_thenConstructExpectedRequestAndReturnResult() {
    MetadataPage metadataPage = getObjectMetadataPage();
    ArrayList<TagFilter> filters = new ArrayList<>();
    String key = "key";
    String value = "123";
    filters.add(TagFilter.with(key, value, LESS_THAN));
    int pageNumber = 3;
    int pageSize = 50;
    String property = "key1";
    Sort.Order direction = Sort.Order.DESC;
    Sort sort = Sort.by(property, direction);
    PageRequest pageRequest =
        PageRequest.builder().page(pageNumber).size(pageSize).sort(sort).build();
    String uriString =
        testServiceUris.get(0).getSecretShareUri().toString()
            + "?"
            + FILTER_PARAMETER
            + "="
            + key
            + "%3C"
            + value
            + "&"
            + PAGE_NUMBER_PARAMETER
            + "="
            + pageNumber
            + "&"
            + PAGE_SIZE_PARAMETER
            + "="
            + pageSize
            + "&"
            + SORT_PROPERTY_PARAMETER
            + "="
            + property
            + "&"
            + SORT_DIRECTION_PARAMETER
            + "="
            + direction.name();
    when(amphoraCommunicationClient.download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class)))
        .thenReturn(Try.success(metadataPage));
    MetadataPage secrets = amphoraClient.getSecrets(filters, pageRequest);
    verify(amphoraCommunicationClient)
        .download(
            argThat(
                (ArgumentMatcher<RequestParameters>) r -> r.getUri().toString().equals(uriString)),
            eq(MetadataPage.class));
    assertThat(secrets.getContent(), is(equalTo(metadataPage.getContent())));
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenListTags_thenReturnExpectedResult() {
    Tag[] tagArray = getTags().toArray(new Tag[0]);
    ArgumentCaptor<RequestParameters> requestCaptor =
        ArgumentCaptor.forClass(RequestParameters.class);
    when(amphoraCommunicationClient.download(requestCaptor.capture(), eq(Tag[].class)))
        .thenReturn(Try.success(tagArray));
    List<Tag> result = amphoraClient.getTags(testSecretId);
    String expectedPath =
        testServiceUris.get(0).getSecretShareResourceTagsUri(testSecretId).getPath();
    assertThat(requestCaptor.getValue().getUri().getPath(), is(equalTo(expectedPath)));
    assertThat(result.size(), is(equalTo(getTags().size())));
    assertThat(result.get(0), is(equalTo(getTags().get(0))));
    assertThat(result.get(1), is(equalTo(getTags().get(1))));
  }

  @Test
  public void givenCommunicationClientFails_whenGetTag_thenThrowAmphoraClientException() {
    String expectedRandomKey = "tagKey";
    when(amphoraCommunicationClient.download(any(RequestParameters.class), eq(Tag.class)))
        .thenReturn(Try.failure(new RuntimeException()));
    AmphoraClientException actualAce =
        assertThrows(
            AmphoraClientException.class,
            () -> amphoraClient.getTag(testSecretId, expectedRandomKey));
    assertEquals(
        String.format(
            FETCHING_TAG_WITH_KEY_FOR_SECRET_FAILED_EXCEPTION_MSG, expectedRandomKey, testSecretId),
        actualAce.getMessage());
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenCreateTag_thenExecuteExpectedRequest() {
    when(amphoraCommunicationClient.upload(requestParamsWithBodyCaptor.capture(), eq(URI.class)))
        .thenReturn(getSuccessUriResponseMap());
    amphoraClient.createTag(testSecretId, testTag);
    String expectedPath =
        testServiceUris.get(0).getSecretShareResourceTagsUri(testSecretId).getPath();
    List<RequestParametersWithBody<Tag>> actualParams = requestParamsWithBodyCaptor.getValue();
    assertThat(actualParams.size(), is(equalTo(testServiceUris.size())));
    assertTrue(actualParams.stream().allMatch(r -> r.getBody().equals(testTag)));
    assertThat(actualParams.get(0).getUri().getPath(), is(equalTo(expectedPath)));
    assertThat(actualParams.get(1).getUri().getPath(), is(equalTo(expectedPath)));
  }

  @Test
  public void givenRequestReturnsFailure_whenCreateTag_thenThrowAmphoraClientException() {
    when(amphoraCommunicationClient.upload(
            argThat(
                (ArgumentMatcher<List<RequestParametersWithBody<Tag>>>)
                    l -> l.stream().allMatch(r -> r.getBody().equals(testTag))),
            eq(URI.class)))
        .thenReturn(getErrorUriResponseMap());
    AmphoraClientException actualAce =
        assertThrows(
            AmphoraClientException.class, () -> amphoraClient.createTag(testSecretId, testTag));
    assertThat(
        actualAce.getMessage(),
        CoreMatchers.endsWith(
            String.format(REQUEST_FOR_ENDPOINT_FAILED_EXCEPTION_MSG, testUri, 500)));
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenOverwriteTags_thenPerformExpectedRequest() {
    List<Tag> tags = new ArrayList<>();
    tags.add(testTag);
    amphoraClient.overwriteTags(testSecretId, tags);
    verify(amphoraCommunicationClient, times(1)).update(requestParamsWithBodyCaptor.capture());
    String expectedPath =
        testServiceUris.get(0).getSecretShareResourceTagsUri(testSecretId).getPath();
    List<RequestParametersWithBody<Tag>> actualParams = requestParamsWithBodyCaptor.getValue();
    assertThat(actualParams.size(), is(equalTo(testServiceUris.size())));
    assertThat(actualParams.get(0).getUri().getPath(), is(equalTo(expectedPath)));
    assertThat(actualParams.get(1).getUri().getPath(), is(equalTo(expectedPath)));
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenGetTag_thenReturnExpectedResult() {
    String tagKey = testTag.getKey();
    ArgumentCaptor<RequestParameters> requestCaptor =
        ArgumentCaptor.forClass(RequestParameters.class);
    when(amphoraCommunicationClient.download(requestCaptor.capture(), eq(Tag.class)))
        .thenReturn(Try.success(testTag));
    Tag result = amphoraClient.getTag(testSecretId, tagKey);
    String expectedPath =
        testServiceUris.get(0).getSecretShareResourceUriTagResource(testSecretId, tagKey).getPath();
    assertThat(requestCaptor.getValue().getUri().getPath(), is(equalTo(expectedPath)));
    assertThat(result, is(equalTo(testTag)));
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenUpdateTag_thenPerformExpectedRequest() {
    amphoraClient.updateTag(testSecretId, testTag);
    verify(amphoraCommunicationClient, times(1)).update(requestParamsWithBodyCaptor.capture());
    String expectedPath =
        testServiceUris
            .get(0)
            .getSecretShareResourceUriTagResource(testSecretId, testTag.getKey())
            .getPath();
    List<RequestParametersWithBody<Tag>> actualParams = requestParamsWithBodyCaptor.getValue();
    assertThat(actualParams.size(), is(equalTo(testServiceUris.size())));
    assertThat(actualParams.get(0).getUri().getPath(), is(equalTo(expectedPath)));
    assertThat(actualParams.get(1).getUri().getPath(), is(equalTo(expectedPath)));
  }

  @SneakyThrows
  @Test
  public void givenSuccessfulRequest_whenDeleteTag_thenPerformExpectedRequest() {
    String tagKey = testTag.getKey();
    ArgumentCaptor<List<RequestParameters>> requestCaptor = ArgumentCaptor.forClass(List.class);
    amphoraClient.deleteTag(testSecretId, tagKey);
    verify(amphoraCommunicationClient, times(1)).delete(requestCaptor.capture());
    String expectedPath =
        testServiceUris.get(0).getSecretShareResourceUriTagResource(testSecretId, tagKey).getPath();
    assertThat(requestCaptor.getValue().size(), is(equalTo(testServiceUris.size())));
    assertThat(requestCaptor.getValue().get(0).getUri().getPath(), is(equalTo(expectedPath)));
    assertThat(requestCaptor.getValue().get(1).getUri().getPath(), is(equalTo(expectedPath)));
  }

  @Test
  public void givenRequestReturnsFailure_whenDeleteTag_thenThrowAmphoraClientException() {
    String expectedErrorDetails = "Secret Not found";
    when(amphoraCommunicationClient.delete(any()))
        .thenReturn(
            Collections.singletonList(
                Try.failure(new AmphoraClientException(expectedErrorDetails))));
    AmphoraClientException actualAce =
        assertThrows(AmphoraClientException.class, () -> amphoraClient.deleteSecret(testSecretId));
    assertThat(
        actualAce.getMessage(),
        CoreMatchers.allOf(
            CoreMatchers.startsWith("At least one request has failed"),
            CoreMatchers.containsString(expectedErrorDetails)));
  }

  @SneakyThrows
  private Map<URI, OutputDeliveryObject> getOutputDeliveryObjectsForSecrets(
      UUID id, BigInteger[] secrets) {
    ByteBuffer p0ShareData = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p0Rs = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p0Us = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p0Vs = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p0Ws = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p1ShareData = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p1Rs = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p1Us = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p1Vs = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    ByteBuffer p1Ws = ByteBuffer.allocate(secrets.length * WORD_WIDTH);
    // <w> = <y * r> and <u> = <v * r>
    for (BigInteger s : secrets) {
      BigInteger r = BigInteger.valueOf(random.nextLong());
      BigInteger v = BigInteger.valueOf(random.nextLong());
      BigInteger w = s.multiply(r).mod(spdzUtil.getPrime());
      BigInteger u = v.multiply(r).mod(spdzUtil.getPrime());
      simpleShareToByteBuffer(s, p0ShareData, p1ShareData);
      simpleShareToByteBuffer(r, p0Rs, p1Rs);
      simpleShareToByteBuffer(v, p0Vs, p1Vs);
      simpleShareToByteBuffer(w, p0Ws, p1Ws);
      simpleShareToByteBuffer(u, p0Us, p1Us);
    }
    Map<URI, OutputDeliveryObject> outputObjects = new HashMap<>();
    outputObjects.put(
        new URI(testUri),
        OutputDeliveryObject.builder()
            .secretId(id)
            .secretShares(p0ShareData.array())
            .rShares(p0Rs.array())
            .vShares(p0Vs.array())
            .wShares(p0Ws.array())
            .uShares(p0Us.array())
            .tags(getTags())
            .build());
    outputObjects.put(
        new URI(testUri2),
        OutputDeliveryObject.builder()
            .secretId(id)
            .secretShares(p1ShareData.array())
            .rShares(p1Rs.array())
            .vShares(p1Vs.array())
            .wShares(p1Ws.array())
            .uShares(p1Us.array())
            .tags(getTags())
            .build());
    return outputObjects;
  }

  private void simpleShareToByteBuffer(BigInteger secret, ByteBuffer target1, ByteBuffer target2) {
    BigInteger mask = new BigInteger(spdzUtil.getPrime().bitCount() - 1, random);
    target1.put(spdzUtil.toGfp(mask));
    target2.put(spdzUtil.toGfp(secret.subtract(mask).mod(spdzUtil.getPrime())));
  }

  @SneakyThrows
  private Map<URI, TupleList> getInputMaskShares(int numberOfMasks) {
    Map<URI, TupleList> inputMaskShares = new HashMap<>();
    TupleList inputMaskListA =
        new TupleList<>(
            TupleType.INPUT_MASK_GFP.getTupleCls(), TupleType.INPUT_MASK_GFP.getField());
    TupleList inputMaskListB =
        new TupleList<>(
            TupleType.INPUT_MASK_GFP.getTupleCls(), TupleType.INPUT_MASK_GFP.getField());
    IntStream.range(0, numberOfMasks)
        .forEach(
            i -> {
              if (random.nextBoolean()) {
                inputMaskListA.add(getMaskA());
                inputMaskListB.add(getMaskB());
              } else {
                inputMaskListA.add(getMaskB());
                inputMaskListB.add(getMaskA());
              }
            });
    inputMaskShares.put(new URI(testUri), inputMaskListA);
    inputMaskShares.put(new URI(testUri2), inputMaskListB);
    return inputMaskShares;
  }

  private InputMask<Field.Gfp> getMaskA() {
    return new InputMask(
        TupleType.INPUT_MASK_GFP.getField(),
        new Share(
            spdzUtil.toGfp(new BigInteger("82730997414791468496799367418496881908")),
            spdzUtil.toGfp(new BigInteger("60557275363670854182192939229091375859"))));
  }

  private InputMask<Field.Gfp> getMaskB() {
    return new InputMask(
        TupleType.INPUT_MASK_GFP.getField(),
        new Share(
            spdzUtil.toGfp(new BigInteger("45359004002536205186084333850157344582")),
            spdzUtil.toGfp(new BigInteger("48604663536222227589564560476962533035"))));
  }

  @SneakyThrows
  private <T> Map<URI, Try<T>> getSuccessUriResponseMap() {
    Map<URI, Try<T>> uriHttpStatusMap = new HashMap<>();
    uriHttpStatusMap.put(new URI(testUri), Try.success(null));
    uriHttpStatusMap.put(new URI(testUri2), Try.success(null));
    return uriHttpStatusMap;
  }

  @SneakyThrows
  private <T> Map<URI, Try<T>> getErrorUriResponseMap() {
    Map<URI, Try<T>> uriHttpStatusMap = new HashMap<>();
    uriHttpStatusMap.put(new URI(testUri), Try.failure(new CsHttpClientException("500")));
    uriHttpStatusMap.put(new URI(testUri2), Try.success(null));
    return uriHttpStatusMap;
  }
}
