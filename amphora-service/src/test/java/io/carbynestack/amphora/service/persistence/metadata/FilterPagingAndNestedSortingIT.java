/*
 * Copyright (c) 2023 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import static io.carbynestack.amphora.common.TagFilterOperator.*;
import static io.carbynestack.amphora.service.persistence.metadata.TagEntity.setFromTagList;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.carbynestack.amphora.common.Metadata;
import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagFilter;
import io.carbynestack.amphora.common.TagValueType;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.carbynestack.amphora.service.AmphoraServiceApplication;
import io.carbynestack.amphora.service.testconfig.PersistenceTestEnvironment;
import io.carbynestack.amphora.service.testconfig.ReusableMinioContainer;
import io.carbynestack.amphora.service.testconfig.ReusablePostgreSQLContainer;
import io.carbynestack.amphora.service.testconfig.ReusableRedisContainer;
import io.carbynestack.amphora.service.util.MetadataMatchers;
import io.vavr.Tuple2;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.Base58;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    classes = {AmphoraServiceApplication.class})
@ActiveProfiles(profiles = {"test"})
@Testcontainers
public class FilterPagingAndNestedSortingIT {
  public static final String LONG_KEY = "LONG_KEY";

  private final UUID testSecretId = UUID.fromString("3bcf8308-8f50-4d24-a37b-b0075bb5e779");
  private final UUID testSecretId2 = UUID.fromString("0e7cd962-d98e-4eea-82ae-4641399c9ad7");
  private final UUID testSecretId3 = UUID.fromString("e043fb9b-6124-437a-a59b-ce988c5ce1c5");
  private final Tag testTag = Tag.builder().key("TEST_KEY").value("TEST_VALUE").build();
  private final Tag testTag2 = Tag.builder().key("SUPER_KEY").value("MY#SUPER,VALUE").build();
  private final Tag testTag3 = Tag.builder().key("SUPER_KEY").value("OTHER~VALUE").build();
  private final Tag testTagLongValue99 = Tag.builder().key(LONG_KEY).value("99").build();
  private final Tag testTagLongValue100 = Tag.builder().key(LONG_KEY).value("100").build();

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

  @Autowired private SecretEntityRepository secretEntityRepository;

  @Autowired private PersistenceTestEnvironment testEnvironment;

  @BeforeEach
  public void setUp() {
    testEnvironment.clearAllData();
  }

  private void persistObjectWithIdAndTags(UUID id, Tag... tags) {
    secretEntityRepository.save(
        new SecretEntity().setSecretId(id.toString()).assignTags(setFromTagList(asList(tags))));
  }

  @Test
  void
      givenMultipleObjectsStoredButOnlyOneMatchingTagFilter_whenGetObjectListWithFilter_thenReturnExpectedContent() {
    persistObjectWithIdAndTags(testSecretId, testTag, testTag2);
    persistObjectWithIdAndTags(testSecretId2, testTag, testTag3);
    List<TagFilter> tagFilters =
        Collections.singletonList(TagFilter.with(testTag2.getKey(), testTag2.getValue(), EQUALS));
    Page<Metadata> secretList = storageService.getSecretList(tagFilters, Sort.unsorted());
    assertEquals(1, secretList.getTotalElements());
    Metadata actualMetadata = secretList.getContent().get(0);
    assertEquals(testSecretId, actualMetadata.getSecretId());
    assertThat(actualMetadata.getTags(), containsInAnyOrder(testTag, testTag2));
  }

  @Test
  void
      givenMultipleObjectsStoredButNoneMatchingFilterCriteria_whenGetObjectListWithFilter_thenReturnExpectedContent() {
    persistObjectWithIdAndTags(testSecretId, testTag, testTag2);
    persistObjectWithIdAndTags(testSecretId2, testTag, testTag3);
    List<TagFilter> tagFilters =
        Arrays.asList(
            TagFilter.with(testTag2.getKey(), testTag2.getValue(), EQUALS),
            TagFilter.with(testTag3.getKey(), testTag3.getValue(), EQUALS));
    Page<Metadata> secretList = storageService.getSecretList(tagFilters, Sort.unsorted());
    assertEquals(0, secretList.getTotalElements());
  }

  @Test
  void
      givenMultipleObjectsStoredMatchingFilterCriteria_whenGetObjectListWithFilter_thenReturnExpectedContent() {
    Metadata[] expectedMetadataItems = {
      Metadata.builder().secretId(testSecretId).tags(asList(testTag, testTag2)).build(),
      Metadata.builder().secretId(testSecretId2).tags(asList(testTag, testTag3)).build()
    };
    for (Metadata metadata : expectedMetadataItems) {
      persistObjectWithIdAndTags(metadata.getSecretId(), metadata.getTags().toArray(new Tag[0]));
    }
    persistObjectWithIdAndTags(testSecretId3, testTag2, testTag3);

    List<TagFilter> tagFilters =
        Collections.singletonList(TagFilter.with(testTag.getKey(), testTag.getValue(), EQUALS));
    Page<Metadata> metadataPage = storageService.getSecretList(tagFilters, Sort.unsorted());
    assertEquals(2, metadataPage.getTotalElements());
    assertThat(
        metadataPage.getContent(),
        containsInAnyOrder(
            MetadataMatchers.equalsButIgnoreReservedTags(expectedMetadataItems[0]),
            MetadataMatchers.equalsButIgnoreReservedTags(expectedMetadataItems[1])));
  }

  @Test
  void
      givenObjectTagsWithLongValues_whenGetObjectListWithLongLessThanFilter_thenReturnExpectedResult() {
    persistObjectWithIdAndTags(testSecretId, testTagLongValue99);
    persistObjectWithIdAndTags(testSecretId2, testTagLongValue100);
    List<TagFilter> tagFilters =
        Collections.singletonList(TagFilter.with(LONG_KEY, "1000", LESS_THAN));
    Page<Metadata> secretList = storageService.getSecretList(tagFilters, Sort.unsorted());
    assertEquals(2, secretList.getTotalElements());
    assertTrue(
        secretList.getContent().stream()
            .anyMatch(metadata -> testSecretId.equals(metadata.getSecretId())));
    assertTrue(
        secretList.getContent().stream()
            .anyMatch(metadata -> testSecretId2.equals(metadata.getSecretId())));
  }

  @Test
  void givenObjectsWithMixedTagValueTypes_whenGetObjectListWithFilter_thenReturnExpectedContent() {
    persistObjectWithIdAndTags(testSecretId, testTagLongValue99);
    persistObjectWithIdAndTags(testSecretId2, testTag, testTagLongValue100);
    List<TagFilter> tagFilters =
        Collections.singletonList(TagFilter.with(LONG_KEY, "1000", LESS_THAN));
    Page<Metadata> secretList = storageService.getSecretList(tagFilters, Sort.unsorted());
    assertEquals(2, secretList.getTotalElements());
    assertTrue(
        secretList.getContent().stream()
            .anyMatch(metadata -> testSecretId.equals(metadata.getSecretId())));
    assertTrue(
        secretList.getContent().stream()
            .anyMatch(metadata -> testSecretId2.equals(metadata.getSecretId())));
  }

  @Test
  void
      givenObjectTagsWithLongValues_whenGetObjectListWithLongGreaterThanFilter_thenReturnExpectedResult() {
    persistObjectWithIdAndTags(testSecretId, testTag2, testTagLongValue99);
    persistObjectWithIdAndTags(testSecretId2, testTagLongValue100);
    List<TagFilter> tagFilters =
        Collections.singletonList(TagFilter.with(LONG_KEY, "99", GREATER_THAN));
    Page<Metadata> secretList = storageService.getSecretList(tagFilters, Sort.unsorted());
    assertEquals(1, secretList.getTotalElements());
    assertTrue(
        secretList.getContent().stream()
            .anyMatch(metadata -> testSecretId2.equals(metadata.getSecretId())));
  }

  @Test
  void
      givenTagValueEqualToGreaterThanFilterValue_whenGetObjectListWithFilter_thenDoNotReturnRelatedObject() {
    persistObjectWithIdAndTags(testSecretId, testTag, testTagLongValue99);
    persistObjectWithIdAndTags(testSecretId2, testTag, testTagLongValue100);
    List<TagFilter> tagFilters =
        Collections.singletonList(TagFilter.with(LONG_KEY, "99", GREATER_THAN));
    Page<Metadata> secretList = storageService.getSecretList(tagFilters, Sort.unsorted());
    assertEquals(1, secretList.getTotalElements());
    assertTrue(
        secretList.getContent().stream()
            .anyMatch(metadata -> testSecretId2.equals(metadata.getSecretId())));
  }

  @Test
  void
      givenNumberComparisonFilterWithNonParsableValue_whenCreateTagFilter_thenThrowIllegalArgumentException() {
    persistObjectWithIdAndTags(testSecretId, testTagLongValue99);
    persistObjectWithIdAndTags(testSecretId2, testTagLongValue100);
    IllegalArgumentException iae =
        assertThrows(
            IllegalArgumentException.class, () -> TagFilter.with(LONG_KEY, "abc1000", LESS_THAN));
    assertEquals("Value cannot be parsed as long.", iae.getMessage());
  }

  @Test
  void givenTagsWithLongValue_whenRequestingAscSortedList_thenReturnProperResult() {
    String tagKey = Base58.randomString(5);
    Metadata[] expectedMetadata = getSortLongTestMetadata(tagKey, Sort.Direction.ASC);
    Page<Metadata> actualMetadataPage = storageService.getSecretList(Sort.by(tagKey).ascending());
    assertThat(
        actualMetadataPage.get().collect(Collectors.toList()),
        containsInRelativeOrder(
            Arrays.stream(expectedMetadata)
                .map(MetadataMatchers::equalsButIgnoreReservedTags)
                .collect(Collectors.toList())));
  }

  @Test
  void givenTagsWithLongValue_whenRequestingDescSortedList_thenReturnProperResult() {
    String tagKey = Base58.randomString(5);
    Metadata[] expectedMetadata = getSortLongTestMetadata(tagKey, Sort.Direction.DESC);
    Page<Metadata> actualMetadataPage = storageService.getSecretList(Sort.by(tagKey).descending());
    assertThat(
        actualMetadataPage.get().collect(Collectors.toList()),
        containsInRelativeOrder(expectedMetadata));
  }

  @Test
  void givenTagsWithStringValue_whenRequestingAscSortedList_thenReturnProperResult() {
    String tagKey = Base58.randomString(5);
    Metadata[] expectedMetadata = getSortStringTestMetadata(tagKey, Sort.Direction.ASC);
    Page<Metadata> actualMetadataPage = storageService.getSecretList(Sort.by(tagKey).ascending());
    assertThat(
        actualMetadataPage.get().collect(Collectors.toList()),
        containsInRelativeOrder(
            Arrays.stream(expectedMetadata)
                .map(MetadataMatchers::equalsButIgnoreReservedTags)
                .collect(Collectors.toList())));
  }

  @Test
  void givenTagsWithStringValue_whenRequestingDescSortedList_thenReturnProperResult() {
    String tagKey = Base58.randomString(5);
    Metadata[] expectedMetadata = getSortStringTestMetadata(tagKey, Sort.Direction.DESC);
    Page<Metadata> actualMetadataPage = storageService.getSecretList(Sort.by(tagKey).descending());
    assertThat(
        actualMetadataPage.get().collect(Collectors.toList()),
        containsInRelativeOrder(
            Arrays.stream(expectedMetadata)
                .map(MetadataMatchers::equalsButIgnoreReservedTags)
                .collect(Collectors.toList())));
  }

  @Test
  void givenMultipleEntitiesButMixedValueTypes_whenRequestingSortedList_thenThrowException() {
    String tagKey = Base58.randomString(5);
    Metadata longTypeMetadata =
        Metadata.builder()
            .secretId(testSecretId)
            .tags(
                Collections.singletonList(
                    Tag.builder()
                        .key(tagKey)
                        .value(Long.toString(123L))
                        .valueType(TagValueType.LONG)
                        .build()))
            .build();
    Metadata stringTypeMetadata =
        Metadata.builder()
            .secretId(testSecretId2)
            .tags(
                Collections.singletonList(
                    Tag.builder()
                        .key(tagKey)
                        .value("textValue")
                        .valueType(TagValueType.STRING)
                        .build()))
            .build();
    Metadata withoutTagMetadata =
        Metadata.builder().secretId(testSecretId3).tags(Collections.emptyList()).build();
    Metadata[] unorderedMetadata =
        new Metadata[] {longTypeMetadata, stringTypeMetadata, withoutTagMetadata};
    for (Metadata omd : unorderedMetadata) {
      persistObjectWithIdAndTags(omd.getSecretId(), omd.getTags().toArray(new Tag[0]));
    }
    Sort sortConfig = Sort.by(tagKey).ascending();
    AmphoraServiceException ase =
        assertThrows(AmphoraServiceException.class, () -> storageService.getSecretList(sortConfig));
    assertEquals(
        "The TagValueType of the tags used for sorting have different configurations.",
        ase.getMessage());
  }

  @Test
  void givenNoTagWithSortingKey_whenRequestingSortedList_thenActAsNoSortingApplied() {
    String actualTagKey = Base58.randomString(5);
    String wrongTagKey = Base58.randomString(5) + "wrong";
    Metadata[] expectedMetadata = getUnorderedStringTestMetadata(actualTagKey);
    Page<Metadata> actualMetadataPage =
        storageService.getSecretList(Sort.by(wrongTagKey).ascending());
    assertThat(
        actualMetadataPage.get().collect(Collectors.toList()),
        containsInRelativeOrder(
            Arrays.stream(expectedMetadata)
                .map(MetadataMatchers::equalsButIgnoreReservedTags)
                .collect(Collectors.toList())));
  }

  private Metadata[] getSortLongTestMetadata(String tagKey, Sort.Direction direction) {
    Metadata[] sortedMetadataList =
        Stream.of(
                new Tuple2<>(UUID.fromString("d6eff51a-a231-4605-92ee-4ee453108e2a"), "13"),
                new Tuple2<>(UUID.fromString("8de2989e-14fa-4481-85c1-70e2a7ed1b19"), "479"),
                new Tuple2<>(UUID.fromString("47ef38bb-7130-487f-8fa5-748ebb765f8b"), "789"))
            .map(
                tuple ->
                    Metadata.builder()
                        .secretId(tuple._1)
                        .tags(
                            Collections.singletonList(
                                Tag.builder()
                                    .key(tagKey)
                                    .value(tuple._2)
                                    .valueType(TagValueType.LONG)
                                    .build()))
                        .build())
            .toArray(Metadata[]::new);
    Metadata metadataWithoutTag =
        Metadata.builder()
            .secretId(UUID.fromString("c0a371e5-da1d-4de7-a748-fd7b1a5ac51f"))
            .tags(Collections.emptyList())
            .build();
    Metadata[] unorderedMetadata =
        new Metadata[] {
          sortedMetadataList[2], sortedMetadataList[0], metadataWithoutTag, sortedMetadataList[1]
        };
    for (Metadata omd : unorderedMetadata) {
      persistObjectWithIdAndTags(omd.getSecretId(), omd.getTags().toArray(new Tag[0]));
    }
    return direction == Sort.Direction.ASC
        ? ArrayUtils.toArray(
            sortedMetadataList[0], sortedMetadataList[1], sortedMetadataList[2], metadataWithoutTag)
        : ArrayUtils.toArray(
            metadataWithoutTag,
            sortedMetadataList[2],
            sortedMetadataList[1],
            sortedMetadataList[0]);
  }

  private Metadata[] getUnorderedStringTestMetadata(String tagKey) {
    Metadata[] sortedMetadataList =
        Stream.of(
                new Tuple2<>(UUID.fromString("d6eff51a-a231-4605-92ee-4ee453108e2a"), "bFj4g"),
                new Tuple2<>(UUID.fromString("8de2989e-14fa-4481-85c1-70e2a7ed1b19"), "dbxhR"),
                new Tuple2<>(UUID.fromString("47ef38bb-7130-487f-8fa5-748ebb765f8b"), "dBxhR"))
            .map(
                tuple ->
                    Metadata.builder()
                        .secretId(tuple._1)
                        .tags(
                            Collections.singletonList(
                                Tag.builder()
                                    .key(tagKey)
                                    .value(tuple._2)
                                    .valueType(TagValueType.STRING)
                                    .build()))
                        .build())
            .toArray(Metadata[]::new);
    Metadata metadataWithoutTag =
        Metadata.builder()
            .secretId(UUID.fromString("c0a371e5-da1d-4de7-a748-fd7b1a5ac51f"))
            .tags(Collections.emptyList())
            .build();
    Metadata[] unorderedMetadata =
        new Metadata[] {
          sortedMetadataList[2], sortedMetadataList[0], metadataWithoutTag, sortedMetadataList[1]
        };
    for (Metadata omd : unorderedMetadata) {
      persistObjectWithIdAndTags(omd.getSecretId(), omd.getTags().toArray(new Tag[0]));
    }
    return unorderedMetadata;
  }

  private Metadata[] getSortStringTestMetadata(String tagKey, Sort.Direction direction) {
    Metadata[] unorderedStringTestMetadata = getUnorderedStringTestMetadata(tagKey);
    return direction == Sort.Direction.ASC
        ? ArrayUtils.toArray(
            unorderedStringTestMetadata[1],
            unorderedStringTestMetadata[3],
            unorderedStringTestMetadata[0],
            unorderedStringTestMetadata[2])
        : ArrayUtils.toArray(
            unorderedStringTestMetadata[2],
            unorderedStringTestMetadata[0],
            unorderedStringTestMetadata[3],
            unorderedStringTestMetadata[1]);
  }
}
