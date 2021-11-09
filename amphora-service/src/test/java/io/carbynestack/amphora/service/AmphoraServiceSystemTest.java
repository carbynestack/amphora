/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service;

import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.INTRA_VCP_OPERATIONS_SEGMENT;
import static io.carbynestack.amphora.common.rest.AmphoraRestApiEndpoints.SECRET_SHARES_ENDPOINT;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.MetadataPage;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.testconfig.PersistenceTestEnvironment;
import io.carbynestack.amphora.service.testconfig.ReusableMinioContainerExtension;
import io.carbynestack.amphora.service.testconfig.ReusablePostgreSQLContainerExtension;
import io.carbynestack.amphora.service.testconfig.ReusableRedisContainerExtension;
import io.carbynestack.amphora.service.util.MetadataMatchers;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    classes = {AmphoraServiceApplication.class})
@ActiveProfiles(profiles = {"test"})
@Slf4j
class AmphoraServiceSystemTest {
  private final UUID testSecretId1 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb26");
  private final UUID testSecretId2 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb27");
  private final UUID testSecretId3 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb28");
  private final UUID testSecretId4 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb29");

  @RegisterExtension
  public static ReusableRedisContainerExtension reusableRedisContainer =
      ReusableRedisContainerExtension.getInstance();

  @RegisterExtension
  public static ReusableMinioContainerExtension reusableMinioContainer =
      ReusableMinioContainerExtension.getInstance();

  @RegisterExtension
  public static ReusablePostgreSQLContainerExtension reusablePostgreSQLContainer =
      ReusablePostgreSQLContainerExtension.getInstance();

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private PersistenceTestEnvironment testEnvironment;
  private final SecretShare testSecretShare1 =
      SecretShare.builder()
          .secretId(testSecretId1)
          .data(
              RandomUtils.nextBytes(
                  RandomUtils.nextInt(1, 100) * MpSpdzIntegrationUtils.SHARE_WIDTH))
          .tags(
              asList(
                  Tag.builder().key("key1").value("value1%,$").build(),
                  Tag.builder().key("number").value("123").build()))
          .build();
  private final SecretShare secretShare2 =
      SecretShare.builder()
          .secretId(testSecretId2)
          .data(
              RandomUtils.nextBytes(
                  RandomUtils.nextInt(1, 100) * MpSpdzIntegrationUtils.SHARE_WIDTH))
          .tags(
              asList(
                  Tag.builder().key("key1").value("value2,").build(),
                  Tag.builder().key("number").value("100").build()))
          .build();
  private final SecretShare secretShare3 =
      SecretShare.builder()
          .secretId(testSecretId3)
          .data(
              RandomUtils.nextBytes(
                  RandomUtils.nextInt(1, 100) * MpSpdzIntegrationUtils.SHARE_WIDTH))
          .tags(
              asList(
                  Tag.builder().key("key1").value("value1%,$").build(),
                  Tag.builder().key("number").value("112").build()))
          .build();
  private final SecretShare secretShare4 =
      SecretShare.builder()
          .secretId(testSecretId4)
          .data(
              RandomUtils.nextBytes(
                  RandomUtils.nextInt(1, 100) * MpSpdzIntegrationUtils.SHARE_WIDTH))
          .tags(Collections.singletonList(Tag.builder().key("empty-value").value("").build()))
          .build();

  @BeforeEach
  public void setUp() {
    testEnvironment.clearAllData();
  }

  @Test
  void givenSuccessfulRequest_whenPostSecretShares_thenReturnExpectedContent() {
    String expectedPath =
        INTRA_VCP_OPERATIONS_SEGMENT
            + SECRET_SHARES_ENDPOINT
            + "/"
            + testSecretShare1.getSecretId();
    URI response =
        restTemplate.postForObject(
            INTRA_VCP_OPERATIONS_SEGMENT + SECRET_SHARES_ENDPOINT, testSecretShare1, URI.class);
    assertEquals("Response contains not expected content!", expectedPath, response.getPath());
  }

  @Test
  void givenSuccessfulRequest_whenListSecrets_thenReturnExpectedContent() {
    createTestSecretShares();
    MetadataPage response = restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        4L, response.getTotalElements(), "Response contains wrong total number of results!");
    assertThat(
        response.getContent().get(0),
        MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(testSecretShare1)));
  }

  @Test
  void
      givenSuccessfulRequest_whenListSecretsWithNumberComparisonFilter_thenReturnExpectedContent() {
    createTestSecretShares();
    MetadataPage response =
        restTemplate.getForObject(
            SECRET_SHARES_ENDPOINT + "?filter=number>100,number<120", MetadataPage.class);
    assertEquals(
        1L, response.getTotalElements(), "Response contains wrong total number of results!");
    assertThat(
        response.getContent().get(0),
        MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(secretShare3)));
  }

  @Test
  void givenSuccessfulRequest_whenListSecretsWithStringEqualFilter_thenReturnExpectedContent() {
    createTestSecretShares();
    MetadataPage response =
        restTemplate.getForObject(
            SECRET_SHARES_ENDPOINT + "?filter=key1:value2%2C", MetadataPage.class);
    assertEquals(
        1L, response.getTotalElements(), "Response contains wrong total number of results!");
    assertThat(
        response.getContent().get(0),
        MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(secretShare2)));
  }

  @Test
  void
      givenSuccessfulRequest_whenListSecretsWithStringAndNumberComparisonFilters_thenReturnExpectedContent() {
    createTestSecretShares();
    MetadataPage response =
        restTemplate.getForObject(
            SECRET_SHARES_ENDPOINT + "?filter=key1:value1%25%2C%24,number>120", MetadataPage.class);
    assertEquals(
        1L, response.getTotalElements(), "Response contains wrong total number of results!");
    assertThat(
        response.getContent().get(0),
        MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(testSecretShare1)));
  }

  @Test
  void givenSuccessfulRequest_whenListSecretsWithNPaginationConfig_thenReturnExpectedContent() {
    createTestSecretShares();
    MetadataPage response =
        restTemplate.getForObject(
            SECRET_SHARES_ENDPOINT + "?pageNumber=1&pageSize=1", MetadataPage.class);
    assertEquals(
        4L, response.getTotalElements(), "Response contains wrong total number of results!");
    assertThat(
        response.getContent().get(0),
        MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(secretShare2)));
  }

  @Test
  void givenSuccessfulRequest_whenDeleteSecret_thenReturnRemoveAndDoNoLongerReturn()
      throws URISyntaxException {
    createTestSecretShares();
    MetadataPage responsePreDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        4L,
        responsePreDel.getTotalElements(),
        "Response contains wrong total number of results prior to deletion!");
    restTemplate.delete(
        new URI(String.format("/secret-shares/%s", testSecretShare1.getSecretId())));
    MetadataPage responsePostDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        3L,
        responsePostDel.getTotalElements(),
        "Response contains wrong total number of results after deletion!");
  }

  @Test
  void givenUnknownSecretId_whenDeleteSecretShare_thenDoNotTouchOtherSecrets()
      throws URISyntaxException {
    createTestSecretShares();
    MetadataPage responsePreDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        4L,
        responsePreDel.getTotalElements(),
        "Response contains wrong total number of results prior to deletion!");

    restTemplate.delete(
        new URI(String.format("/secret-shares/%s", "82a73814-321c-4261-abcd-27c6c0ebfb20")));

    MetadataPage responsePostDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        4L,
        responsePostDel.getTotalElements(),
        "Response contains wrong total number of results after fake deletion!");
  }

  @Test
  void givenSuccessfulRequest_whenListSecretsWithNSorting_thenReturnExpectedContent() {
    createTestSecretShares();
    MetadataPage response =
        restTemplate.getForObject(
            SECRET_SHARES_ENDPOINT + "?sort=key1&dir=DESC", MetadataPage.class);
    assertEquals(
        4L, response.getTotalElements(), "Response contains wrong total number of results!");
    assertThat(
        response.getContent(),
        hasItems(
            MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(secretShare4)),
            MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(secretShare2)),
            MetadataMatchers.equalsButIgnoreReservedTags(
                getMetadataForSecretShare(testSecretShare1)),
            MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(secretShare3))));
  }

  private void createTestSecretShares() {
    restTemplate.postForObject(
        INTRA_VCP_OPERATIONS_SEGMENT + SECRET_SHARES_ENDPOINT, testSecretShare1, URI.class);
    restTemplate.postForObject(
        INTRA_VCP_OPERATIONS_SEGMENT + SECRET_SHARES_ENDPOINT, secretShare2, URI.class);
    restTemplate.postForObject(
        INTRA_VCP_OPERATIONS_SEGMENT + SECRET_SHARES_ENDPOINT, secretShare3, URI.class);
    restTemplate.postForObject(
        INTRA_VCP_OPERATIONS_SEGMENT + SECRET_SHARES_ENDPOINT, secretShare4, URI.class);
  }

  private Metadata getMetadataForSecretShare(SecretShare secretShare) {
    return Metadata.builder()
        .secretId(secretShare.getSecretId())
        .tags(secretShare.getTags())
        .build();
  }
}
