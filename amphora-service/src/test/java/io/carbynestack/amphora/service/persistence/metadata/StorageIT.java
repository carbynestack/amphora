/*
 * Copyright (c) 2023-2024 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import static io.carbynestack.amphora.service.persistence.datastore.MinioSecretShareDataStore.GET_DATA_FOR_SECRET_EXCEPTION_MSG;
import static io.carbynestack.amphora.service.persistence.metadata.StorageService.NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG;
import static io.carbynestack.amphora.service.persistence.metadata.StorageService.NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG;
import static io.carbynestack.amphora.service.persistence.metadata.TagEntity.setFromTagList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.TagFilterOperator;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.AmphoraServiceApplication;
import io.carbynestack.amphora.service.config.MinioProperties;
import io.carbynestack.amphora.service.exceptions.CsOpaException;
import io.carbynestack.amphora.service.exceptions.NotFoundException;
import io.carbynestack.amphora.service.exceptions.UnauthorizedException;
import io.carbynestack.amphora.service.opa.OpaClient;
import io.carbynestack.amphora.service.testconfig.PersistenceTestEnvironment;
import io.carbynestack.amphora.service.testconfig.ReusableMinioContainer;
import io.carbynestack.amphora.service.testconfig.ReusablePostgreSQLContainer;
import io.carbynestack.amphora.service.testconfig.ReusableRedisContainer;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    classes = {AmphoraServiceApplication.class})
@ActiveProfiles(profiles = {"test"})
@Testcontainers
public class StorageIT {
  private final UUID unknownId = UUID.fromString("d6d0f4ff-df28-4c96-b7df-95170320eaee");
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
  private final UUID testSecretId2 = UUID.fromString("0e7cd962-d98e-4eea-82ae-4641399c9ad7");
  private final Tag testTag = Tag.builder().key("TEST_KEY").value("TEST_VALUE").build();
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

  @Autowired private StorageService storageService;

  @Autowired private PersistenceTestEnvironment testEnvironment;

  @Autowired private SecretEntityRepository secretEntityRepository;

  @Autowired private TagRepository tagRepository;

  @Autowired private MinioClient minioClient;
  @Autowired private MinioProperties minioProperties;

  @MockBean
  private OpaClient opaClientMock;

  @BeforeEach
  public void setUp() {
    testEnvironment.clearAllData();
    when(opaClientMock.newRequest()).thenCallRealMethod();
    when(opaClientMock.isAllowed(any(), any(), eq(authorizedUserId), any())).thenReturn(true);
  }

  private SecretEntity persistObjectWithIdAndTags(UUID id, Tag... tags) {
    return secretEntityRepository.save(
        new SecretEntity().setSecretId(id.toString()).assignTags(setFromTagList(asList(tags))));
  }

  @Test
  void givenSuccessfulRequest_whenStoreTag_thenPersist() throws CsOpaException, UnauthorizedException {
    persistObjectWithIdAndTags(testSecretId, testTag);
    storageService.storeTag(
        testSecretId, Tag.builder().key("ANOTHER_KEY").value(testTag.getValue()).build(),
        authorizedUserId);

    assertEquals(testTag,
            storageService.retrieveTag(testSecretId, testTag.getKey(),authorizedUserId));
  }

  @Test
  void givenSuccessfulRequest_whenGetObjectList_thenReturnExpectedResult() {
    Metadata expectedMetadata =
        Metadata.builder().secretId(testSecretId).tags(singletonList(testTag)).build();
    persistObjectWithIdAndTags(testSecretId, testTag);
    Page<Metadata> actualMetadataPage = storageService.getSecretList(Sort.unsorted());
    assertEquals(singletonList(expectedMetadata), actualMetadataPage.getContent());
  }

  @Test
  void givenNoObjectWithReferencedIdDefined_whenRetrieveTags_thenThrowNotFoundException() {
    NotFoundException nfe =
        assertThrows(NotFoundException.class, () ->
                storageService.retrieveTags(testSecretId, authorizedUserId));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenSecretIdUnknown_whenStoreTag_thenThrowNotFoundException() {
    NotFoundException nfe =
        assertThrows(NotFoundException.class, () ->
                storageService.storeTag(unknownId, testTag, authorizedUserId));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, unknownId), nfe.getMessage());
  }

  @Test
  void givenSuccessfulRequests_whenStoreAndRetrieveTag_thenPersistAndReturnExpectedContent() throws CsOpaException, UnauthorizedException {
    persistObjectWithIdAndTags(testSecretId);
    storageService.storeTag(testSecretId, testTag, authorizedUserId);
    List<Tag> tags = storageService.retrieveTags(testSecretId, authorizedUserId);
    assertEquals(testTag, tags.get(0));
    assertEquals(1, tags.size());
  }

  @Test
  void givenObjectWithoutTagsDefined_whenRetrieveTags_thenReturnEmptyList() throws CsOpaException, UnauthorizedException {
    persistObjectWithIdAndTags(testSecretId);
    assertEquals(Collections.emptyList(), storageService.retrieveTags(testSecretId, authorizedUserId));
  }

  @SneakyThrows
  @Test
  void givenObjectWithoutTagsDefined_whenRetrieveSecretShare_thenReturnEmptyListForTags() {
    persistObjectWithIdAndTags(testSecretId);
    minioClient.putObject(
        PutObjectArgs.builder()
            .bucket(minioProperties.getBucket())
            .object(testSecretId.toString())
            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
            .build());
    assertEquals(Collections.emptyList(),
            storageService.getSecretShare(testSecretId, authorizedUserId).getTags());
  }

  @Test
  void givenObjectHasNoDataPersisted_whenGetSecretShare_thenThrowAmphoraServiceException() {
    persistObjectWithIdAndTags(testSecretId);
    AmphoraServiceException ase =
        assertThrows(
            AmphoraServiceException.class, () ->
                        storageService.getSecretShare(testSecretId, authorizedUserId));
    assertEquals(
        String.format(
            GET_DATA_FOR_SECRET_EXCEPTION_MSG, testSecretId, "The specified key does not exist."),
        ase.getMessage());
  }

  @Test
  void givenSecretIdUnknown_whenReplaceTags_thenThrowNotFoundException() {
    List<Tag> tags = singletonList(testTag);
    NotFoundException nfe =
        assertThrows(
            NotFoundException.class,
            () -> {
              storageService.replaceTags(unknownId, tags, authorizedUserId);
            });
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, unknownId), nfe.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenDeleteTag_thenDoNoLongerReturn() throws CsOpaException, UnauthorizedException {
    SecretEntity secretEntity = persistObjectWithIdAndTags(testSecretId, testTag);
    Tag expectedTag = Tag.builder().key(testTag.getKey() + "new").value(testTag.getValue()).build();
    TagEntity expectedTagEntity = TagEntity.fromTag(expectedTag).setSecret(secretEntity);
    tagRepository.save(expectedTagEntity);
    persistObjectWithIdAndTags(testSecretId2, testTag);

    storageService.deleteTag(testSecretId, testTag.getKey(), authorizedUserId);

    List<Tag> actualTags = storageService.retrieveTags(testSecretId, authorizedUserId);
    assertEquals(1, actualTags.size());
    assertEquals(expectedTag, actualTags.get(0));
    assertEquals(
        1,
        storageService
            .getSecretList(
                singletonList(
                    TagFilter.with(testTag.getKey(), testTag.getValue(), TagFilterOperator.EQUALS)),
                Sort.unsorted())
            .getTotalElements());
    assertEquals(2, storageService.getSecretList(Sort.unsorted()).getTotalElements());
  }

  @Test
  void givenObjectHasNoTagWithRequestedKey_whenDeleteKey_thenThrowNotFoundException() {
    String unknownKey = "unknown_key";
    persistObjectWithIdAndTags(testSecretId, testTag);
    persistObjectWithIdAndTags(testSecretId2, testTag);

    NotFoundException nfe =
        assertThrows(
            NotFoundException.class, () ->
                        storageService.deleteTag(testSecretId, unknownKey, authorizedUserId));
    assertEquals(
        String.format(
            NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG, unknownKey, testSecretId),
        nfe.getMessage());
  }

  @Test
  void givenMultipleObjectsWIthIdenticalTag_whenDeleteTagOnOneObject_thenKeepTagForOtherObjects() throws CsOpaException, UnauthorizedException {
    persistObjectWithIdAndTags(testSecretId, testTag);
    persistObjectWithIdAndTags(testSecretId2, testTag);
    storageService.deleteTag(testSecretId, testTag.getKey(), authorizedUserId);

    assertTrue(isEmpty(storageService.retrieveTags(testSecretId, authorizedUserId)));
    assertEquals(
        1,
        storageService
            .getSecretList(
                singletonList(
                    TagFilter.with(testTag.getKey(), testTag.getValue(), TagFilterOperator.EQUALS)),
                Sort.unsorted())
            .getTotalElements());
    assertEquals(2, storageService.getSecretList(Sort.unsorted()).getTotalElements());
  }

  @Test
  void givenAnUnknownId_whenStoringATag_thenThrow() {
    NotFoundException nfe =
        assertThrows(NotFoundException.class, () -> storageService.storeTag(testSecretId, testTag, authorizedUserId));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }
}
