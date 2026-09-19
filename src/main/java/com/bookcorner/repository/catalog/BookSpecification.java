package com.bookcorner.repository.catalog;

import com.bookcorner.entity.catalog.BookAuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic JPA Specification builder for complex catalog faceted searching and filtering.
 */
public class BookSpecification {

    private BookSpecification() {
        // Utility class
    }

    public static Specification<BookEntity> withFilters(
            UUID categoryId,
            String authorSlug,
            String formatType,
            Long minPrice,
            Long maxPrice,
            String language
    ) {
        return (root, query, criteriaBuilder) -> {
            // Prevent duplicate records when joining collections in count/paged queries
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            // 1. Filter by category
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("primaryCategory").get("id"), categoryId));
            }

            // 2. Filter by author slug
            if (authorSlug != null && !authorSlug.isBlank()) {
                Join<BookEntity, BookAuthorEntity> bookAuthors = root.join("bookAuthors", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(bookAuthors.get("author").get("authorSlug"), authorSlug));
            }

            // 3 & 4. Filter by format type and price range
            boolean hasFormatType = formatType != null && !formatType.isBlank();
            boolean hasPrice = minPrice != null || maxPrice != null;
            if (hasFormatType || hasPrice) {
                Join<BookEntity, BookFormatEntity> formats = root.join("formats", JoinType.INNER);
                if (hasFormatType) {
                    predicates.add(criteriaBuilder.equal(formats.get("formatType"), formatType));
                }
                if (minPrice != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(formats.get("basePriceAmount"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(formats.get("basePriceAmount"), maxPrice));
                }
            }

            // 5. Filter by language
            if (language != null && !language.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("primaryLanguage"), language));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
