/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.metadata;

import static io.carbynestack.amphora.service.persistence.metadata.StorageService.*;
import static io.carbynestack.amphora.service.util.CreationDateTagMatchers.containsCreationDateTag;
import static io.carbynestack.castor.common.entities.Field.GFP;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

import io.carbynestack.amphora.common.*;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.calculation.SecretShareConverter;
import io.carbynestack.amphora.service.calculation.SecretShareConverterFactory;
import io.carbynestack.amphora.service.config.AmphoraServiceProperties;
import io.carbynestack.amphora.service.config.SpdzProperties;
import io.carbynestack.amphora.service.exceptions.AlreadyExistsException;
import io.carbynestack.amphora.service.exceptions.NotFoundException;
import io.carbynestack.amphora.service.persistence.cache.InputMaskCachingService;
import io.carbynestack.amphora.service.persistence.datastore.SecretShareDataStore;
import io.carbynestack.castor.common.entities.InputMask;
import io.carbynestack.castor.common.entities.TupleList;
import io.carbynestack.mpspdz.integration.MpSpdzIntegrationUtils;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {
  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
  private final UUID testSecretId2 = UUID.fromString("0e7cd962-d98e-4eea-82ae-4641399c9ad7");
  private final Tag testTag = Tag.builder().key("TEST_KEY").value("TEST_VALUE").build();
  private final Tag testTag2 = Tag.builder().key("SUPER_KEY").value("MY#SUPER,VALUE").build();
  private final Tag testTagReservedCreationDateKey =
      Tag.builder().key(StorageService.RESERVED_TAG_KEYS.get(0)).value("MY#SUPER,VALUE").build();

  @Mock private SecretEntityRepository secretEntityRepository;
  @Mock private InputMaskCachingService inputMaskStore;
  @Mock private TagRepository tagRepository;
  @Mock private SecretShareConverterFactory converterFactory;
  @Mock private SecretShareConverter converter;
  @Mock private SecretShareDataStore secretShareDataStore;

  @InjectMocks private StorageService storageService;

  @Test
  void givenIdIsAlreadyInUse_whenCreateSecret_thenThrowAlreadyExistsException() {
    MaskedInput testMaskedInput =
        new MaskedInput(
            testSecretId,
            singletonList(
                MaskedInputData.of(RandomUtils.nextBytes(MpSpdzIntegrationUtils.WORD_WIDTH))),
            singletonList(testTag));

    when(secretEntityRepository.existsById(testMaskedInput.getSecretId().toString()))
        .thenReturn(true);

    AlreadyExistsException aee =
        assertThrows(
            AlreadyExistsException.class, () -> storageService.createSecret(testMaskedInput));
    assertEquals(SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, aee.getMessage());
  }

  @Test
  void givenMaskedInputHasTagsWithSameKey_whenCreateObject_thenThrowIllegalArgumentException() {
    Tag tagWithSameKeyAsTestTag =
        Tag.builder()
            .key(testTag.getKey())
            .value("duplicate")
            .valueType(TagValueType.STRING)
            .build();
    MaskedInput maskedInputDuplicateTagKeys =
        new MaskedInput(
            testSecretId,
            singletonList(
                MaskedInputData.of(RandomUtils.nextBytes(MpSpdzIntegrationUtils.WORD_WIDTH))),
            asList(testTag, tagWithSameKeyAsTestTag));

    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> storageService.createSecret(maskedInputDuplicateTagKeys));
    assertEquals(TAGS_WITH_THE_SAME_KEY_DEFINED_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  void givenMaskedInputWithReservedKey_whenCreateObject_thenReplaceReservedKey() {
    MaskedInput maskedInput =
        new MaskedInput(
            testSecretId,
            singletonList(
                MaskedInputData.of(RandomUtils.nextBytes(MpSpdzIntegrationUtils.WORD_WIDTH))),
            asList(testTag, testTagReservedCreationDateKey));
    TupleList expectedInputMasks =
        new TupleList<>(InputMask.class, ShareFamily.COWGEAR.getFamilyName(), GFP);
    SecretShare expectedSecretShare =
        SecretShare.builder()
            .secretId(maskedInput.getSecretId())
            .data(RandomUtils.nextBytes(MpSpdzIntegrationUtils.SHARE_WIDTH))
            .tags(maskedInput.getTags())
            .build();
    RuntimeException expectedAbortTestException = new RuntimeException("No need to go further");
    ArgumentCaptor<SecretEntity> secretEntityArgumentCaptor =
        ArgumentCaptor.forClass(SecretEntity.class);

    when(inputMaskStore.getCachedInputMasks(maskedInput.getSecretId()))
        .thenReturn(expectedInputMasks);
    when(converterFactory.createShareConverter(Optional.empty())).thenReturn(converter);
    when(converter.convert(maskedInput, expectedInputMasks, ShareFamily.COWGEAR))
        .thenReturn(expectedSecretShare);
    when(secretEntityRepository.save(secretEntityArgumentCaptor.capture()))
        .thenThrow(expectedAbortTestException);

    assertThrows(
        RuntimeException.class,
        () -> storageService.createSecret(maskedInput),
        expectedAbortTestException.getMessage());
    SecretEntity capturedSecretEntity = secretEntityArgumentCaptor.getValue();
    List<Tag> actualTags = TagEntity.setToTagList(capturedSecretEntity.getTags());
    assertEquals(2, actualTags.size());
    MatcherAssert.assertThat(actualTags, allOf(hasItem(testTag), containsCreationDateTag()));
    Tag actualTagWithReservedKey =
        actualTags.stream()
            .filter(t -> t.getKey().equals(testTagReservedCreationDateKey.getKey()))
            .findFirst()
            .get();
    assertNotEquals(testTagReservedCreationDateKey, actualTagWithReservedKey);
  }

  @Test
  void givenSuccessfulRequest_whenCreateObject_thenReturnSecretId() {
    MaskedInput maskedInput =
        new MaskedInput(
            testSecretId,
            singletonList(
                MaskedInputData.of(RandomUtils.nextBytes(MpSpdzIntegrationUtils.WORD_WIDTH))),
            singletonList(testTag));
    TupleList expectedInputMasks =
        new TupleList<>(InputMask.class, ShareFamily.COWGEAR.getFamilyName(), GFP);
    SecretShare expectedSecretShare =
        SecretShare.builder()
            .secretId(maskedInput.getSecretId())
            .data(RandomUtils.nextBytes(MpSpdzIntegrationUtils.SHARE_WIDTH))
            .tags(maskedInput.getTags())
            .build();
    SecretEntity persistedSecretEntity =
        new SecretEntity(
            maskedInput.getSecretId().toString(), TagEntity.setFromTagList(maskedInput.getTags()));
    ArgumentCaptor<SecretEntity> secretEntityArgumentCaptor =
        ArgumentCaptor.forClass(SecretEntity.class);

    when(secretEntityRepository.save(any())).thenReturn(persistedSecretEntity);
    when(inputMaskStore.getCachedInputMasks(maskedInput.getSecretId()))
        .thenReturn(expectedInputMasks);
    when(converterFactory.createShareConverter(Optional.empty())).thenReturn(converter);
    when(converter.convert(maskedInput, expectedInputMasks, ShareFamily.COWGEAR))
        .thenReturn(expectedSecretShare);
    when(secretEntityRepository.save(secretEntityArgumentCaptor.capture()))
        .thenReturn(persistedSecretEntity);

    assertEquals(maskedInput.getSecretId().toString(), storageService.createSecret(maskedInput));
    verify(secretEntityRepository, times(1)).existsById(maskedInput.getSecretId().toString());
    verify(inputMaskStore, times(1)).getCachedInputMasks(maskedInput.getSecretId());
    verify(secretEntityRepository, times(1)).save(secretEntityArgumentCaptor.capture());
    verify(secretShareDataStore, times(1))
        .storeSecretShareData(expectedSecretShare.getSecretId(), expectedSecretShare.getData());
    verify(inputMaskStore, times(1)).removeInputMasks(maskedInput.getSecretId());
    SecretEntity actualSecretEntity = secretEntityArgumentCaptor.getValue();
    assertEquals(maskedInput.getSecretId().toString(), actualSecretEntity.getSecretId());
    List<Tag> actualTags = TagEntity.setToTagList(actualSecretEntity.getTags());
    assertEquals(2, actualTags.size());
    MatcherAssert.assertThat(
        actualTags,
        allOf(containsCreationDateTag(), hasItems(maskedInput.getTags().toArray(new Tag[0]))));
  }

  @Test
  void givenIdIsAlreadyInUse_whenStoreSecretShare_thenThrowAlreadyExistsException() {
    SecretShare secretShare =
        SecretShare.builder()
            .secretId(testSecretId)
            .data(RandomUtils.nextBytes(MpSpdzIntegrationUtils.SHARE_WIDTH))
            .tags(Collections.emptyList())
            .build();

    when(secretEntityRepository.existsById(secretShare.getSecretId().toString())).thenReturn(true);

    AlreadyExistsException aee =
        assertThrows(
            AlreadyExistsException.class, () -> storageService.storeSecretShare(secretShare));
    assertEquals(SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, aee.getMessage());
  }

  @Test
  void givenSecretShareHasTagsWithSameKey_whenStoreObject_thenThrowIllegalArgumentException() {
    Tag tagWithSameKeyAsTestTag =
        Tag.builder()
            .key(testTag.getKey())
            .value("duplicate")
            .valueType(TagValueType.STRING)
            .build();
    SecretShare secretShare =
        SecretShare.builder()
            .secretId(testSecretId)
            .data(RandomUtils.nextBytes(MpSpdzIntegrationUtils.SHARE_WIDTH))
            .tags(asList(testTag, tagWithSameKeyAsTestTag))
            .build();

    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> storageService.storeSecretShare(secretShare));
    assertEquals(TAGS_WITH_THE_SAME_KEY_DEFINED_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  void givenSecretShareWithReservedKey_whenStoreSecretShare_thenReplaceReservedKey() {
    SecretShare secretShare =
        SecretShare.builder()
            .secretId(testSecretId)
            .data(RandomUtils.nextBytes(MpSpdzIntegrationUtils.SHARE_WIDTH))
            .tags(asList(testTag, testTagReservedCreationDateKey))
            .build();
    RuntimeException expectedAbortTestException = new RuntimeException("No need to go further");
    ArgumentCaptor<SecretEntity> secretEntityArgumentCaptor =
        ArgumentCaptor.forClass(SecretEntity.class);

    when(secretEntityRepository.save(secretEntityArgumentCaptor.capture()))
        .thenThrow(expectedAbortTestException);

    assertThrows(
        RuntimeException.class,
        () -> storageService.storeSecretShare(secretShare),
        expectedAbortTestException.getMessage());
    SecretEntity capturedSecretEntity = secretEntityArgumentCaptor.getValue();
    List<Tag> actualTags = TagEntity.setToTagList(capturedSecretEntity.getTags());
    assertEquals(2, actualTags.size());
    MatcherAssert.assertThat(actualTags, allOf(hasItem(testTag), containsCreationDateTag()));
    //noinspection OptionalGetWithoutIsPresent
    Tag actualTagWithReservedKey =
        actualTags.stream()
            .filter(t -> t.getKey().equals(testTagReservedCreationDateKey.getKey()))
            .findFirst()
            .get();
    assertNotEquals(testTagReservedCreationDateKey, actualTagWithReservedKey);
  }

  @Test
  void givenSuccessfulRequest_whenStoreSecretShare_thenReturnSecretId() {
    SecretShare secretShare =
        SecretShare.builder()
            .secretId(testSecretId)
            .data(RandomUtils.nextBytes(MpSpdzIntegrationUtils.SHARE_WIDTH))
            .tags(asList(testTag, testTagReservedCreationDateKey))
            .build();
    SecretEntity secretEntity = new SecretEntity(testSecretId.toString(), emptySet());

    when(secretEntityRepository.save(any())).thenReturn(secretEntity);

    assertEquals(
        secretShare.getSecretId().toString(), storageService.storeSecretShare(secretShare));
  }

  @Test
  void givenSuccessfulRequest_whenGetObjectListWithPageable_thenReturnExpectedContent() {
    List<Metadata> expectedMetadataList =
        asList(
            Metadata.builder().secretId(testSecretId).tags(singletonList(testTag)).build(),
            Metadata.builder().secretId(testSecretId2).tags(singletonList(testTag2)).build());
    Page<SecretEntity> expectedObjectEntityPage =
        new PageImpl<>(
            expectedMetadataList.stream()
                .map(
                    om ->
                        new SecretEntity(
                            om.getSecretId().toString(), TagEntity.setFromTagList(om.getTags())))
                .collect(Collectors.toList()));
    Pageable pageable = Pageable.unpaged();

    when(secretEntityRepository.findAll(pageable)).thenReturn(expectedObjectEntityPage);

    assertEquals(expectedMetadataList, storageService.getSecretList(pageable).getContent());
  }

  @Test
  void givenSuccessfulRequest_whenGetObjectListWithSortConfig_thenReturnExpectedContent() {
    List<Metadata> expectedMetadataList =
        asList(
            Metadata.builder().secretId(testSecretId).tags(singletonList(testTag)).build(),
            Metadata.builder().secretId(testSecretId2).tags(singletonList(testTag2)).build());
    Page<SecretEntity> expectedObjectEntityPage =
        new PageImpl<>(
            expectedMetadataList.stream()
                .map(
                    om ->
                        new SecretEntity(
                            om.getSecretId().toString(), TagEntity.setFromTagList(om.getTags())))
                .collect(Collectors.toList()));
    Sort sort = Sort.by("SortProperty");
    ArgumentCaptor<Pageable> pageableArgumentCaptor = ArgumentCaptor.forClass(Pageable.class);

    when(secretEntityRepository.findAll(pageableArgumentCaptor.capture()))
        .thenReturn(expectedObjectEntityPage);

    assertEquals(expectedMetadataList, storageService.getSecretList(sort).getContent());
    Pageable actualPageable = pageableArgumentCaptor.getValue();
    assertEquals(PageRequest.of(0, Integer.MAX_VALUE, sort), actualPageable);
  }

  @Test
  void givenSuccessfulRequest_whenGetObjectListWithFilterAndPageable_thenReturnExpectedContent() {
    List<TagFilter> tagFilters =
        singletonList(
            TagFilter.with(testTag.getKey(), testTag.getValue(), TagFilterOperator.EQUALS));
    SecretEntitySpecification expectedSpecification = SecretEntitySpecification.with(tagFilters);
    List<Metadata> expectedMetadataList =
        asList(
            Metadata.builder().secretId(testSecretId).tags(singletonList(testTag)).build(),
            Metadata.builder().secretId(testSecretId2).tags(singletonList(testTag2)).build());
    Page<SecretEntity> expectedObjectEntityPage =
        new PageImpl<>(
            expectedMetadataList.stream()
                .map(
                    om ->
                        new SecretEntity(
                            om.getSecretId().toString(), TagEntity.setFromTagList(om.getTags())))
                .collect(Collectors.toList()));
    Pageable pageable = Pageable.unpaged();

    when(secretEntityRepository.findAll(expectedSpecification, pageable))
        .thenReturn(expectedObjectEntityPage);

    assertEquals(
        expectedMetadataList, storageService.getSecretList(tagFilters, pageable).getContent());
  }

  @Test
  void givenSuccessfulRequest_whenGetObjectListWithFilterAndSortConfig_thenReturnExpectedContent() {
    List<TagFilter> tagFilters =
        singletonList(
            TagFilter.with(testTag.getKey(), testTag.getValue(), TagFilterOperator.EQUALS));
    SecretEntitySpecification expectedSpecification = SecretEntitySpecification.with(tagFilters);
    List<Metadata> expectedMetadataList =
        asList(
            Metadata.builder().secretId(testSecretId).tags(singletonList(testTag)).build(),
            Metadata.builder().secretId(testSecretId2).tags(singletonList(testTag2)).build());
    Page<SecretEntity> expectedObjectEntityPage =
        new PageImpl<>(
            expectedMetadataList.stream()
                .map(
                    om ->
                        new SecretEntity(
                            om.getSecretId().toString(), TagEntity.setFromTagList(om.getTags())))
                .collect(Collectors.toList()));
    Sort sort = Sort.by("SortProperty");
    ArgumentCaptor<Pageable> pageableArgumentCaptor = ArgumentCaptor.forClass(Pageable.class);

    when(secretEntityRepository.findAll(
            eq(expectedSpecification), pageableArgumentCaptor.capture()))
        .thenReturn(expectedObjectEntityPage);

    assertEquals(expectedMetadataList, storageService.getSecretList(tagFilters, sort).getContent());
    Pageable actualPageable = pageableArgumentCaptor.getValue();
    assertEquals(PageRequest.of(0, Integer.MAX_VALUE, sort), actualPageable);
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenGetSecretShare_thenThrowNotFoundException() {
    when(secretEntityRepository.findById(testSecretId.toString())).thenReturn(Optional.empty());

    NotFoundException nfe =
        assertThrows(NotFoundException.class, () -> storageService.getSecretShare(testSecretId));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenDataCannotBeRetrieved_whenGetSecretShare_thenThrowAmphoraServiceException() {
    AmphoraServiceException expectedAse = new AmphoraServiceException("Expected this one");
    SecretEntity secretEntity = new SecretEntity();
    when(secretEntityRepository.findById(testSecretId.toString()))
        .thenReturn(Optional.of(secretEntity));
    when(secretShareDataStore.getSecretShareData(testSecretId)).thenThrow(expectedAse);

    assertThrows(
        AmphoraServiceException.class,
        () -> storageService.getSecretShare(testSecretId),
        expectedAse.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenGetSecretShare_thenReturnContent() {
    List<Tag> expectedTags = singletonList(testTag);
    byte[] expectedData = RandomUtils.nextBytes(MpSpdzIntegrationUtils.SHARE_WIDTH);
    SecretEntity existingSecretEntity =
        new SecretEntity(testSecretId.toString(), TagEntity.setFromTagList(expectedTags));

    when(secretEntityRepository.findById(existingSecretEntity.getSecretId()))
        .thenReturn(Optional.of(existingSecretEntity));
    when(secretShareDataStore.getSecretShareData(
            UUID.fromString(existingSecretEntity.getSecretId())))
        .thenReturn(expectedData);

    SecretShare actualSecretShare =
        storageService.getSecretShare(UUID.fromString(existingSecretEntity.getSecretId()));
    assertEquals(
        UUID.fromString(existingSecretEntity.getSecretId()), actualSecretShare.getSecretId());
    assertEquals(expectedTags, actualSecretShare.getTags());
    assertEquals(expectedData, actualSecretShare.getData());
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenDeleteObject_thenThrowNotFoundException() {
    when(secretEntityRepository.deleteBySecretId(testSecretId.toString())).thenReturn(0L);

    NotFoundException nfe =
        assertThrows(NotFoundException.class, () -> storageService.deleteSecret(testSecretId));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenDeleteObjectDataFails_whenDeleteObject_thenThrowGivenException() {
    AmphoraServiceException expectedAse = new AmphoraServiceException("Expected this one");
    when(secretEntityRepository.deleteBySecretId(testSecretId.toString())).thenReturn(1L);
    when(secretShareDataStore.deleteSecretShareData(testSecretId)).thenThrow(expectedAse);

    assertThrows(
        AmphoraServiceException.class,
        () -> storageService.deleteSecret(testSecretId),
        expectedAse.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenDeleteObject_thenDeleteObjectAndData() {
    when(secretEntityRepository.deleteBySecretId(testSecretId.toString())).thenReturn(1L);

    storageService.deleteSecret(testSecretId);
    verify(secretShareDataStore, times(1)).deleteSecretShareData(testSecretId);
  }

  @Test
  void givenTagHasReservedKey_whenStoreTag_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> storageService.storeTag(testSecretId, testTagReservedCreationDateKey));
    assertEquals(
        String.format(IS_RESERVED_KEY_EXCEPTION_MSG, testTagReservedCreationDateKey.getKey()),
        iae.getMessage());
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenStoreTag_thenThrowNotFoundException() {
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(false);
    NotFoundException nfe =
        assertThrows(NotFoundException.class, () -> storageService.storeTag(testSecretId, testTag));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenObjectAlreadyHasTagWithGivenKey_whenStoreTag_thenThrowAlreadyExistsException() {
    SecretEntity existingSecretEntity = new SecretEntity().setSecretId(testSecretId.toString());
    TagEntity existingTagEntity = TagEntity.fromTag(testTag);
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(existingSecretEntity.getSecretId()))
        .thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, testTag.getKey()))
        .thenReturn(Optional.of(existingTagEntity));
    AlreadyExistsException aee =
        assertThrows(
            AlreadyExistsException.class, () -> storageService.storeTag(testSecretId, testTag));
    assertEquals(
        String.format(
            TAG_WITH_KEY_EXISTS_FOR_SECRET_EXCEPTION_MSG,
            testTag.getKey(),
            existingSecretEntity.getSecretId()),
        aee.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenStoreTag_thenPersistTag() {
    SecretEntity existingSecretEntity = new SecretEntity().setSecretId(testSecretId.toString());
    TagEntity expectedTagEntity = TagEntity.fromTag(testTag);
    ArgumentCaptor<TagEntity> tagEntityArgumentCaptor = ArgumentCaptor.forClass(TagEntity.class);

    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(existingSecretEntity.getSecretId()))
        .thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, testTag.getKey()))
        .thenReturn(Optional.empty());
    when(tagRepository.save(any())).thenReturn(expectedTagEntity);

    assertEquals(
        expectedTagEntity.getKey(),
        storageService.storeTag(UUID.fromString(existingSecretEntity.getSecretId()), testTag));

    verify(tagRepository, times(1)).save(tagEntityArgumentCaptor.capture());
    TagEntity actualTagEntity = tagEntityArgumentCaptor.getValue();
    assertEquals(testTag, actualTagEntity.toTag());
    assertEquals(existingSecretEntity, actualTagEntity.getSecret());
  }

  @Test
  void givenListHasTagsWithSameKey_whenReplaceTags_thenThrowIllegalArgumentException() {
    Tag tagWithSameKeyAsTestTag =
        Tag.builder()
            .key(testTag.getKey())
            .value("duplicate")
            .valueType(TagValueType.STRING)
            .build();
    List<Tag> invalidTagList = asList(testTag, tagWithSameKeyAsTestTag);

    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> storageService.replaceTags(testSecretId, invalidTagList));
    assertEquals(TAGS_WITH_THE_SAME_KEY_DEFINED_EXCEPTION_MSG, iae.getMessage());
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenReplaceTags_thenThrowNotFoundException() {
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(false);
    List<Tag> emptyTags = emptyList();
    NotFoundException nfe =
        assertThrows(
            NotFoundException.class, () -> storageService.replaceTags(testSecretId, emptyTags));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenListHasTagWithReservedKey_whenReplaceTags_thenReplaceByExistingTagAndPersist() {
    List<Tag> tagListWithReservedKey = asList(testTag, testTagReservedCreationDateKey);
    TagEntity existingCreationTagEntity =
        TagEntity.fromTag(
            Tag.builder()
                .key(testTagReservedCreationDateKey.getKey())
                .value(Long.toString(System.currentTimeMillis() - 1000))
                .valueType(TagValueType.LONG)
                .build());
    SecretEntity existingSecretEntity = new SecretEntity(testSecretId.toString(), emptySet());
    ArgumentCaptor<Set<TagEntity>> tagEntitySetArgumentCaptor = ArgumentCaptor.forClass(Set.class);

    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(existingSecretEntity.getSecretId()))
        .thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, existingCreationTagEntity.getKey()))
        .thenReturn(Optional.of(existingCreationTagEntity));

    storageService.replaceTags(
        UUID.fromString(existingSecretEntity.getSecretId()), tagListWithReservedKey);

    verify(tagRepository, times(1)).deleteBySecret(existingSecretEntity);
    verify(tagRepository, times(1)).saveAll(tagEntitySetArgumentCaptor.capture());
    Set<TagEntity> actualTagEntityList = tagEntitySetArgumentCaptor.getValue();
    assertEquals(2, actualTagEntityList.size());
    MatcherAssert.assertThat(
        actualTagEntityList,
        allOf(
            hasItem(TagEntity.fromTag(testTag).setSecret(existingSecretEntity)),
            hasItem(existingCreationTagEntity.setSecret(existingSecretEntity))));
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenRetrieveTags_thenThrowNotFoundException() {
    when(secretEntityRepository.findById(testSecretId.toString())).thenReturn(Optional.empty());

    NotFoundException nfe =
        assertThrows(NotFoundException.class, () -> storageService.retrieveTags(testSecretId));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenSuccessfulRequest_whenRetrieveTags_thenReturnContent() {
    List<Tag> expectedTags = asList(testTag, testTag2);
    SecretEntity existingSecretEntity =
        new SecretEntity(testSecretId.toString(), TagEntity.setFromTagList(expectedTags));
    when(secretEntityRepository.findById(testSecretId.toString()))
        .thenReturn(Optional.of(existingSecretEntity));

    MatcherAssert.assertThat(
        storageService.retrieveTags(UUID.fromString(existingSecretEntity.getSecretId())),
        containsInAnyOrder(expectedTags.toArray()));
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenRetrieveTag_thenThrowNotFoundException() {
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(false);
    String key = testTag.getKey();
    NotFoundException nfe =
        assertThrows(NotFoundException.class, () -> storageService.retrieveTag(testSecretId, key));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenNoTagWithGivenKeyInDatabaseForGivenObject_whenRetrieveTag_thenThrowNotFoundException() {
    String key = testTag.getKey();
    SecretEntity existingSecretEntity = new SecretEntity(testSecretId.toString(), emptySet());
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(testSecretId.toString())).thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, testTag.getKey()))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> storageService.retrieveTag(testSecretId, key),
        String.format(
            NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG,
            testTag.getKey(),
            existingSecretEntity.getSecretId()));
  }

  @Test
  void givenSuccessfulRequest_whenRetrieveTag_thenReturnContent() {
    TagEntity existingTagEntity = TagEntity.fromTag(testTag);
    SecretEntity existingSecretEntity =
        new SecretEntity(testSecretId.toString(), singleton(existingTagEntity));
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(testSecretId.toString())).thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, existingTagEntity.getKey()))
        .thenReturn(Optional.of(existingTagEntity));

    assertEquals(testTag, storageService.retrieveTag(testSecretId, testTag.getKey()));
  }

  @Test
  void givenTagHasReservedKey_whenUpdateTag_thenThrowIllegalArgumentException() {
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> storageService.updateTag(testSecretId, testTagReservedCreationDateKey));
    assertEquals(
        String.format(IS_RESERVED_KEY_EXCEPTION_MSG, testTagReservedCreationDateKey.getKey()),
        iae.getMessage());
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenUpdateTag_thenThrowNotFoundException() {
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(false);

    NotFoundException nfe =
        assertThrows(
            NotFoundException.class, () -> storageService.updateTag(testSecretId, testTag));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenNoTagWithGivenKeyInDatabaseForGivenObject_whenUpdateTag_thenThrowNotFoundException() {
    SecretEntity existingSecretEntity = new SecretEntity(testSecretId.toString(), emptySet());
    when(secretEntityRepository.existsById(existingSecretEntity.getSecretId())).thenReturn(true);
    when(secretEntityRepository.getById(existingSecretEntity.getSecretId()))
        .thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, testTag.getKey()))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> storageService.updateTag(testSecretId, testTag),
        String.format(
            NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG,
            testTag.getKey(),
            existingSecretEntity.getSecretId()));
  }

  @Test
  void givenSuccessfulRequest_whenUpdateTag_thenUpdateTag() {
    Tag newTag =
        Tag.builder().key(testTag.getKey()).value("123").valueType(TagValueType.LONG).build();
    TagEntity existingTagEntity = TagEntity.fromTag(testTag);
    SecretEntity existingSecretEntity =
        new SecretEntity(testSecretId.toString(), singleton(existingTagEntity));
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(testSecretId.toString())).thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, testTag.getKey()))
        .thenReturn(Optional.of(existingTagEntity));

    storageService.updateTag(UUID.fromString(existingSecretEntity.getSecretId()), newTag);

    ArgumentCaptor<TagEntity> tagEntityArgumentCaptor = ArgumentCaptor.forClass(TagEntity.class);
    verify(tagRepository, times(1)).save(tagEntityArgumentCaptor.capture());
    TagEntity actualTagEntity = tagEntityArgumentCaptor.getValue();
    assertEquals(newTag, actualTagEntity.toTag());
    assertEquals(existingSecretEntity, actualTagEntity.getSecret());
  }

  @Test
  void givenTagHasReservedKey_whenDeleteTag_thenThrowIllegalArgumentException() {
    String reservedKey = testTagReservedCreationDateKey.getKey();
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class,
            () -> storageService.deleteTag(testSecretId, reservedKey));
    assertEquals(String.format(IS_RESERVED_KEY_EXCEPTION_MSG, reservedKey), iae.getMessage());
  }

  @Test
  void givenNoSecretShareWithGivenIdInDatabase_whenDeleteTag_thenThrowNotFoundException() {
    String key = testTag.getKey();

    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(false);

    NotFoundException nfe =
        assertThrows(NotFoundException.class, () -> storageService.deleteTag(testSecretId, key));
    assertEquals(
        String.format(NO_SECRET_WITH_ID_EXISTS_EXCEPTION_MSG, testSecretId), nfe.getMessage());
  }

  @Test
  void givenNoTagWithGivenKeyInDatabaseForGivenObject_whenDeleteTag_thenThrowNotFoundException() {
    String key = testTag.getKey();
    SecretEntity existingSecretEntity = new SecretEntity(testSecretId.toString(), emptySet());
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(testSecretId.toString())).thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, key)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> storageService.deleteTag(testSecretId, key),
        String.format(
            NO_TAG_WITH_KEY_EXISTS_FOR_SECRET_WITH_ID_EXCEPTION_MSG,
            testTag.getKey(),
            existingSecretEntity.getSecretId()));
  }

  @Test
  void givenSuccessfulRequest_whenDeleteTag_thenDelete() {
    TagEntity tagEntityToDelete = TagEntity.fromTag(testTag);
    SecretEntity existingSecretEntity =
        new SecretEntity(testSecretId.toString(), singleton(tagEntityToDelete));
    when(secretEntityRepository.existsById(testSecretId.toString())).thenReturn(true);
    when(secretEntityRepository.getById(testSecretId.toString())).thenReturn(existingSecretEntity);
    when(tagRepository.findBySecretAndKey(existingSecretEntity, tagEntityToDelete.getKey()))
        .thenReturn(Optional.of(tagEntityToDelete));

    storageService.deleteTag(
        UUID.fromString(existingSecretEntity.getSecretId()), tagEntityToDelete.getKey());

    verify(tagRepository, times(1)).delete(tagEntityToDelete);
  }
}
