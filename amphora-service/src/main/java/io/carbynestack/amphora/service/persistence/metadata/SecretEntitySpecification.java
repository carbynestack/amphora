/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.persistence.metadata;

import static io.carbynestack.amphora.common.TagFilterOperator.EQUALS;
import static io.carbynestack.amphora.service.persistence.metadata.SecretEntity.SECRET_ID_FIELD;
import static io.carbynestack.amphora.service.persistence.metadata.TagEntity.*;

import io.carbynestack.amphora.common.TagFilter;
import java.util.List;
import java.util.stream.Stream;
import javax.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

/**
 * An implementation of {@link Specification} used to apply {@link TagFilter} based filtering
 * criteria to a {@link CriteriaQuery}.
 */
@EqualsAndHashCode
@AllArgsConstructor(staticName = "with")
public class SecretEntitySpecification implements Specification<SecretEntity> {
  private final List<TagFilter> filterParams;

  @Override
  public Predicate toPredicate(
      @NonNull Root<SecretEntity> root,
      @NonNull CriteriaQuery<?> query,
      @NonNull CriteriaBuilder criteriaBuilder) {
    return filterParams.stream()
        .map(
            filter -> {
              Subquery<TagEntity> subquery = query.subquery(TagEntity.class);
              Root<TagEntity> subqueryRoot = subquery.from(TagEntity.class);

              Predicate secretSelectorPredicate =
                  criteriaBuilder.equal(root.get(SECRET_ID_FIELD), subqueryRoot.get(SECRET_FIELD));
              Predicate filterPredicate =
                  mapToKeyValueFilterStream(filter)
                      .map(tagFilter -> toPredicate(subqueryRoot, criteriaBuilder, tagFilter))
                      .reduce(criteriaBuilder::and)
                      .orElseGet(() -> alwaysTrue(criteriaBuilder));

              return subquery
                  .select(subqueryRoot.get(SECRET_FIELD))
                  .where(criteriaBuilder.and(secretSelectorPredicate, filterPredicate));
            })
        .map(
            tagEntitySubquery ->
                criteriaBuilder.and(
                    criteriaBuilder.in(root.get(SECRET_ID_FIELD)).value(tagEntitySubquery)))
        .reduce(criteriaBuilder::and)
        .orElseGet(() -> alwaysTrue(criteriaBuilder));
  }

  private Stream<TagFilter> mapToKeyValueFilterStream(TagFilter tagFilter) {
    return Stream.of(
        TagFilter.with(KEY_FIELD, tagFilter.getKey(), EQUALS),
        TagFilter.with(VALUE_FIELD, tagFilter.getValue(), tagFilter.getOperator()));
  }

  private Predicate toPredicate(Path<?> path, CriteriaBuilder builder, TagFilter criteria) {
    switch (criteria.getOperator()) {
      case EQUALS:
        return builder.equal(path.get(criteria.getKey()).as(String.class), criteria.getValue());
      case GREATER_THAN:
        return builder.greaterThan(
            path.get(criteria.getKey()).as(Long.class), Long.valueOf(criteria.getValue()));
      case LESS_THAN:
        return builder.lessThan(
            path.get(criteria.getKey()).as(Long.class), Long.valueOf(criteria.getValue()));
      default:
        return null;
    }
  }

  private Predicate alwaysTrue(CriteriaBuilder builder) {
    return builder.isTrue(builder.literal(true));
  }
}
