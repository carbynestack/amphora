/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.carbynestack.amphora.service.persistence.metadata;

import static io.carbynestack.amphora.service.persistence.metadata.TagEntity.*;
import static io.vavr.API.$;
import static io.vavr.API.Case;

import io.carbynestack.amphora.common.Tag;
import io.carbynestack.amphora.common.TagValueType;
import io.carbynestack.amphora.common.exceptions.AmphoraServiceException;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PagingAndNestedSortingObjectEntityRepositoryImpl
    implements PagingAndNestedSortingObjectEntityRepository {
  private final EntityManager entityManager;

  private TypedQuery<SortableObjectEntity> createQuery(SecretEntitySpecification spec, Sort sort) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<SortableObjectEntity> cq = builder.createQuery(SortableObjectEntity.class);
    Root<SecretEntity> root = applySpecification(cq, spec);
    return applySorting(root, cq, sort);
  }

  /**
   * Applies the sorting criteria to a given query.
   *
   * <p>We want to sort based on the value of a tag. However, tags are managed as a nested
   * collection of the secret. In order to reflect this using Spring, we split the filters and
   * sorting into an inner and outer query.
   *
   * <p><b>Sorting must always be applied last.</b>
   */
  private TypedQuery<SortableObjectEntity> applySorting(
      Root<SecretEntity> root, CriteriaQuery<SortableObjectEntity> criteriaQuery, Sort sort) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Option<SortConfig> sortConfigOption = getSortConfig(sort);

    if (sortConfigOption.isDefined()) {
      SortConfig sortConfig = sortConfigOption.get();

      Subquery<TagEntity> subqueryValue = criteriaQuery.subquery(TagEntity.class);
      Root<TagEntity> subRootValue = subqueryValue.from(TagEntity.class);
      subqueryValue
          .select(subRootValue.get(VALUE_FIELD))
          .where(
              builder.and(
                  builder.equal(
                      root.get(SecretEntity.SECRET_ID_FIELD), subRootValue.get(SECRET_FIELD)),
                  builder.equal(subRootValue.get(KEY_FIELD), sortConfig.order.getProperty())));

      return entityManager.createQuery(
          criteriaQuery
              .select(
                  builder.construct(
                      SortableObjectEntity.class,
                      root,
                      subqueryValue.as(
                          sortConfigOption
                              .map(
                                  config -> {
                                    switch (config.tagValueType) {
                                      case LONG:
                                        return Long.class;
                                      case STRING:
                                      default:
                                        return String.class;
                                    }
                                  })
                              .get())))
              .orderBy(
                  sortConfig.order.getDirection().isAscending()
                      ? builder.asc(builder.literal(2))
                      : builder.desc(builder.literal(2))));
    } else {
      return entityManager.createQuery(
          criteriaQuery.select(builder.construct(SortableObjectEntity.class, root)));
    }
  }

  /**
   * Returns the {@link TagValueType} defined for the given sorting tag.
   *
   * @param sort the sort configuration
   * @return the corresponding {@link TagValueType}
   * @throws AmphoraServiceException if no {@link Tag} with the given key exists or if multiple
   *     {@link Tag}s key are defined but with mismatching configuration
   */
  private Option<SortConfig> getSortConfig(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return Option.none();
    }
    Sort.Order order = sort.iterator().next();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<String> tagEntityCriteriaQuery = builder.createQuery(String.class);
    Root<TagEntity> tagEntityRoot = tagEntityCriteriaQuery.from(TagEntity.class);
    //noinspection unchecked
    return Option.of(
        Try.of(
                () ->
                    entityManager
                        .createQuery(
                            tagEntityCriteriaQuery
                                .select(tagEntityRoot.get(VALUE_TYPE_FIELD))
                                .where(
                                    builder.equal(
                                        tagEntityRoot.get(KEY_FIELD), order.getProperty()))
                                .groupBy(tagEntityRoot.get(VALUE_TYPE_FIELD)))
                        .setFirstResult(0)
                        .setMaxResults(2)
                        .getResultList())
            .mapFailure(
                Case(
                    $(cause -> cause != null && cause.getClass().isInstance(Exception.class)),
                    cause ->
                        new AmphoraServiceException(
                            "Error while determining the sorting data type.", cause)))
            .map(
                resultList -> {
                  if (resultList.size() == 1) {
                    return SortConfig.with(order, TagValueType.valueOf(resultList.get(0)));
                  } else if (resultList.size() > 1) {
                    throw new AmphoraServiceException(
                        "The TagValueType of the tags used for sorting have different"
                            + " configurations.");
                  } else {
                    log.debug("Not a single Entity with the given sorting Tag defined.");
                    return null;
                  }
                })
            .get());
  }

  private Root<SecretEntity> applySpecification(
      CriteriaQuery<?> query, SecretEntitySpecification spec) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Root<SecretEntity> root = query.from(SecretEntity.class);
    if (spec != null) {
      Option.of(spec.toPredicate(root, query, builder)).peek(query::where);
    }
    return root;
  }

  private Page<SecretEntity> getObjectEntityPage(
      TypedQuery<SortableObjectEntity> query, Pageable pageable, SecretEntitySpecification spec) {
    query.setFirstResult((int) pageable.getOffset());
    query.setMaxResults(pageable.getPageSize());
    return new PageImpl<>(
        query.getResultList().stream()
            .map(SortableObjectEntity::getSecretEntity)
            .collect(Collectors.toList()),
        pageable,
        executeCountQuery(createCountQuery(spec)));
  }

  private static Long executeCountQuery(TypedQuery<Long> query) {
    return query
        .getResultStream()
        .parallel()
        .mapToLong(entity -> entity != null ? entity : 0L)
        .sum();
  }

  @Value(staticConstructor = "with")
  private static class SortConfig {
    Sort.Order order;
    TagValueType tagValueType;
  }

  protected TypedQuery<Long> createCountQuery(SecretEntitySpecification spec) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = builder.createQuery(Long.class);
    Root<SecretEntity> root = applySpecification(query, spec);
    query.select(builder.count(root));
    return entityManager.createQuery(query);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<SecretEntity> findAll(@NonNull Pageable pageable) {
    TypedQuery<SortableObjectEntity> query = createQuery(null, pageable.getSort());
    return getObjectEntityPage(query, pageable, null);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<SecretEntity> findAll(
      @NonNull SecretEntitySpecification spec, @NonNull Pageable pageable) {
    TypedQuery<SortableObjectEntity> query = createQuery(spec, pageable.getSort());
    return getObjectEntityPage(query, pageable, spec);
  }
}
