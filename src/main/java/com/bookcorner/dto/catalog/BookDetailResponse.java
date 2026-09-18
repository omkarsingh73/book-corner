package com.bookcorner.dto.catalog;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive book detail specification response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailResponse implements Serializable {

    private UUID bookId;
    private String isbn13;
    private String title;
    private String subtitle;
    private String synopsis;
    private PublisherSummary publisher;
    private LocalDate publicationDate;
    private String primaryLanguage;
    private Integer pageCount;
    private String coverImageUrl;
    private CategorySummary primaryCategory;

    @Builder.Default
    private List<AuthorSummary> authors = new ArrayList<>();

    @Builder.Default
    private List<BookFormatDetail> formats = new ArrayList<>();

    private RatingSummary ratingSummary;

    public UUID getId() {
        return bookId;
    }

    public void setId(UUID id) {
        this.bookId = id;
    }

    public static class BookDetailResponseBuilder {
        public BookDetailResponseBuilder id(UUID id) {
            this.bookId = id;
            return this;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublisherSummary implements Serializable {
        private UUID publisherId;
        private String publisherName;
        private String publisherCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary implements Serializable {
        private UUID categoryId;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorSummary implements Serializable {
        private UUID authorId;
        private String name;
        private String authorSlug;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookFormatDetail implements Serializable {
        private UUID formatId;
        private String formatType;
        private String sku;
        private MoneyDto basePrice;
        private Integer inventoryQuantity;
        private Boolean inStock;
        private Integer weightGrams;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingSummary implements Serializable {
        private Double averageRating;
        private Integer totalReviews;
    }
}
