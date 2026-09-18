package com.bookcorner.dto.catalog;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Summary card DTO for book search result and catalog listings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCatalogCardDto implements Serializable {

    private UUID bookId;
    private String isbn13;
    private String title;
    private String subtitle;

    @Builder.Default
    private List<AuthorSummary> authors = new ArrayList<>();

    private CategorySummary primaryCategory;
    private String coverImageUrl;
    private Double averageRating;
    private Integer reviewCount;

    @Builder.Default
    private List<BookFormatSummary> availableFormats = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorSummary implements Serializable {
        private UUID authorId;
        private String name;
        private String role;
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
    public static class BookFormatSummary implements Serializable {
        private UUID formatId;
        private String formatType;
        private String sku;
        private MoneyDto price;
        private Boolean inStock;
    }
}
