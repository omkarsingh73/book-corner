package com.bookcorner.repository.ordering;

import com.bookcorner.entity.ordering.OrderEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic JPA Specification builder for filtering order history.
 */
public class OrderSpecification {

    private OrderSpecification() {
        // Utility class
    }

    public static Specification<OrderEntity> withFilters(
            UUID userId,
            String orderStatus,
            Instant fromDate,
            Instant toDate,
            Long minAmount,
            Long maxAmount
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory user ownership filter if specified
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            // 2. Filter by status
            if (orderStatus != null && !orderStatus.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("orderStatus"), orderStatus));
            }

            // 3. Filter by date range
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            // 4. Filter by amount range
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
