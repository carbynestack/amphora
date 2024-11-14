/*
 * Copyright (c) 2023-2024 - for information on the respective copyright owner
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.MetadataPage;
import io.carbynestack.amphora.common.SecretShare;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.opa.OpaClient;
import io.carbynestack.amphora.service.persistence.metadata.StorageService;
import io.carbynestack.amphora.service.testconfig.PersistenceTestEnvironment;
import io.carbynestack.amphora.service.testconfig.ReusableMinioContainer;
import io.carbynestack.amphora.service.testconfig.ReusablePostgreSQLContainer;
import io.carbynestack.amphora.service.testconfig.ReusableRedisContainer;
import io.carbynestack.amphora.service.util.MetadataMatchers;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    classes = {AmphoraServiceApplication.class})
@ActiveProfiles(profiles = {"test"})
@Slf4j
@Testcontainers
public class AmphoraServiceSystemTest {
  private final UUID testSecretId1 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb26");
  private final UUID testSecretId2 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb27");
  private final UUID testSecretId3 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb28");
  private final UUID testSecretId4 = UUID.fromString("82a73814-321c-4261-abcd-27c6c0ebfb29");
  private final String bearerToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImM5Njk0OTgyLWQzMTAtNDBkOC04ZDk4LTczOWI1ZGZjNWUyNiIsInR5cCI6IkpXVCJ9.eyJhbXIiOlsicGFzc3dvcmQiXSwiYXRfaGFzaCI6InowbjhudTNJQ19HaXN3bmFTWjgwZ2ciLCJhdWQiOlsiOGExYTIwNzUtMzY3Yi00ZGU1LTgyODgtMGMyNzQ1OTMzMmI3Il0sImF1dGhfdGltZSI6MTczMTUwMDQ0OSwiZXhwIjoxNzMxNTA0NDIyLCJpYXQiOjE3MzE1MDA4MjIsImlzcyI6Imh0dHA6Ly8xNzIuMTguMS4xMjguc3NsaXAuaW8vaWFtL29hdXRoIiwianRpIjoiZTlhMmQxYzQtZGViNy00MTgwLWE0M2YtN2QwNTZhYjNlNTk3Iiwibm9uY2UiOiJnV1JVZjhxTERaeDZpOFNhMXpMdm9IX2tSQ01OWll2WTE0WTFsLWNBU0tVIiwicmF0IjoxNzMxNTAwODIyLCJzaWQiOiJlNGVkOTc2Mi0yMmNlLTQyYzEtOTU3NC01MDVkYjAyMThhNDYiLCJzdWIiOiJhZmMwMTE3Zi1jOWNkLTRkOGMtYWNlZS1mYTE0MzNjYTBmZGQifQ.OACqa6WjpAeZbHR54b3p7saUk9plTdXlZsou41E-gfC7WxCG7ZEKfDPKXUky-r20oeIt1Ov3S2QL6Kefe5dTXEC6nhKGxeClg8ys56_FPcx_neI-p09_pSWOkMx7DHP65giaP7UubyyInVpE-2Eu1o6TpoheahNQfCahKDsJmJ-4Vvts3wA79UMfOI0WHO4vLaaG6DRAZQK_dv7ltw3p_WlncpaQAtHwY9iVhtdB3LtAI39EjplCoAF0c9uQO6W7KHWUlj24l2kc564bsJgZSrYvezw6b2-FY7YisVnicSyAORpeqhWEpLltH3D8I1NtHlSYMJhWuVZbBhAm7Iz6q1-W-Q9ttvdPchdwPSASFRkrjrdIyQf6MsFrItKzUxYck57EYL4GkyN9MWvMNxL1UTtkzGsFEczUVsJFm8OQpulYXIFZksmnPTBB0KuUUvEZ-xih8V1HsMRoHvbiCLaDJwjOFKzPevVggsSMysPKR52UAZJDZLTeHBnVCtQ3rro6T0RxNg94lXypz0AmfsGnoTF34u4FmMxzoeFZ9N5zmEpOnMRqLs7Sb3FnLL-IMitc9_2bsHuFbBJl8KbiGHBQihK5v5SIa292L7P9ChsxomWVhM29qHNFuXQMwFUr57hmveNh2Fz9mduZ5h2hLUuDf5xc6u9rSxy3_e3t_xBuUT4";
  private final String authorizedUserId ="afc0117f-c9cd-4d8c-acee-fa1433ca0fdd";

  @Container
  public static ReusableRedisContainer reusableRedisContainer =
      ReusableRedisContainer.getInstance();

  @Container
  public static ReusableMinioContainer reusableMinioContainer =
      ReusableMinioContainer.getInstance();

  @Container
  public static ReusablePostgreSQLContainer reusablePostgreSQLContainer =
      ReusablePostgreSQLContainer.getInstance();

  @MockBean
  private OpaClient opaClientMock;

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
  public void setUp() throws UnauthorizedException {
    testEnvironment.clearAllData();
    when(opaClientMock.newRequest()).thenCallRealMethod();
    when(opaClientMock.isAllowed(any(), any(), eq(authorizedUserId), any())).thenReturn(true);
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
        "Response contains wrong total number of results!", 4L, response.getTotalElements());
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
        "Response contains wrong total number of results!", 1L, response.getTotalElements());
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
        "Response contains wrong total number of results!", 1L, response.getTotalElements());
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
        "Response contains wrong total number of results!", 1L, response.getTotalElements());
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
        "Response contains wrong total number of results!", 4L, response.getTotalElements());
    assertThat(
        response.getContent().get(0),
        MetadataMatchers.equalsButIgnoreReservedTags(getMetadataForSecretShare(secretShare2)));
  }

  @SneakyThrows
  @Test
  void givenSuccessfulRequest_whenDeleteSecret_thenReturnRemoveAndDoNoLongerReturn() {
    createTestSecretShares();
    MetadataPage responsePreDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        "Response contains wrong total number of results prior to deletion!",
        4L,
        responsePreDel.getTotalElements());
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(bearerToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<Void> response = restTemplate.exchange(new URI(String.format("/secret-shares/%s", testSecretShare1.getSecretId())),
            HttpMethod.DELETE, entity, Void.class);
    assertTrue("Request must be successful.", response.getStatusCode().is2xxSuccessful());
    MetadataPage responsePostDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        "Response contains wrong total number of results after deletion!",
        3L,
        responsePostDel.getTotalElements());
  }

  @SneakyThrows
  @Test
  void givenUnknownSecretId_whenDeleteSecretShare_thenDoNotTouchOtherSecrets() {
    createTestSecretShares();
    MetadataPage responsePreDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        "Response contains wrong total number of results prior to deletion!",
        4L,
        responsePreDel.getTotalElements());

    restTemplate.delete(
        new URI(String.format("/secret-shares/%s", "82a73814-321c-4261-abcd-27c6c0ebfb20")));

    MetadataPage responsePostDel =
        restTemplate.getForObject(SECRET_SHARES_ENDPOINT, MetadataPage.class);
    assertEquals(
        "Response contains wrong total number of results after fake deletion!",
        4L,
        responsePostDel.getTotalElements());
  }

  @Test
  void givenSuccessfulRequest_whenListSecretsWithNSorting_thenReturnExpectedContent() {
    createTestSecretShares();
    MetadataPage response =
        restTemplate.getForObject(
            SECRET_SHARES_ENDPOINT + "?sort=key1&dir=DESC", MetadataPage.class);
    assertEquals(
        "Response contains wrong total number of results!", 4L, response.getTotalElements());
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
