package com.bookcorner.mapper;

import com.bookcorner.dto.catalog.AuthorDto;
import com.bookcorner.dto.catalog.BookCatalogCardDto;
import com.bookcorner.dto.catalog.BookDetailResponse;
import com.bookcorner.dto.catalog.CategoryNodeDto;
import com.bookcorner.dto.catalog.PublisherDto;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.entity.catalog.AuthorEntity;
import com.bookcorner.entity.catalog.BookAuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.catalog.CategoryEntity;
import com.bookcorner.entity.catalog.PublisherEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for Catalog master models (Books, Formats, Authors, Publishers, Categories).
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface CatalogMapper {

    // --- Book Catalog Card Mapping ---
    @Mapping(target = "bookId", source = "id")
    @Mapping(target = "authors", source = "bookAuthors", qualifiedByName = "mapBookAuthorsToCardAuthors")
    @Mapping(target = "primaryCategory", source = "primaryCategory", qualifiedByName = "mapCategorySummary")
    @Mapping(target = "availableFormats", source = "formats", qualifiedByName = "mapFormatsToCardFormats")
    @Mapping(target = "averageRating", constant = "0.0")
    @Mapping(target = "reviewCount", constant = "0")
    BookCatalogCardDto toBookCatalogCardDto(BookEntity entity);

    List<BookCatalogCardDto> toBookCatalogCardDtoList(List<BookEntity> entities);

    // --- Book Detail Mapping ---
    @Mapping(target = "bookId", source = "id")
    @Mapping(target = "publisher", source = "publisher", qualifiedByName = "mapPublisherSummary")
    @Mapping(target = "primaryCategory", source = "primaryCategory", qualifiedByName = "mapDetailCategorySummary")
    @Mapping(target = "authors", source = "bookAuthors", qualifiedByName = "mapBookAuthorsToDetailAuthors")
    @Mapping(target = "formats", source = "formats", qualifiedByName = "mapFormatsToDetailFormats")
    @Mapping(target = "ratingSummary", ignore = true)
    BookDetailResponse toBookDetailResponse(BookEntity entity);

    default BookDetailResponse toBookDetailResponse(BookEntity entity, double averageRating, long reviewCount) {
        BookDetailResponse response = toBookDetailResponse(entity);
        if (response != null) {
            response.setRatingSummary(new BookDetailResponse.RatingSummary(averageRating, (int) reviewCount));
        }
        return response;
    }

    // --- Author Mapping ---
    @Mapping(target = "authorId", source = "id")
    AuthorDto toAuthorDto(AuthorEntity entity);

    List<AuthorDto> toAuthorDtoList(List<AuthorEntity> entities);

    // --- Publisher Mapping ---
    @Mapping(target = "publisherId", source = "id")
    PublisherDto toPublisherDto(PublisherEntity entity);

    // --- Category Mapping ---
    @Mapping(target = "categoryId", source = "id")
    @Mapping(target = "name", source = "categoryName")
    @Mapping(target = "slug", source = "categorySlug")
    @Mapping(target = "level", source = "categoryLevel")
    @Mapping(target = "subcategories", source = "subcategories")
    CategoryNodeDto toCategoryNodeDto(CategoryEntity entity);

    List<CategoryNodeDto> toCategoryNodeDtoList(List<CategoryEntity> entities);

    // --- Qualified Helper Methods ---
    @Named("mapCategorySummary")
    default BookCatalogCardDto.CategorySummary mapCategorySummary(CategoryEntity category) {
        if (category == null) return null;
        return BookCatalogCardDto.CategorySummary.builder()
                .categoryId(category.getId())
                .name(category.getCategoryName())
                .slug(category.getCategorySlug())
                .build();
    }

    @Named("mapDetailCategorySummary")
    default BookDetailResponse.CategorySummary mapDetailCategorySummary(CategoryEntity category) {
        if (category == null) return null;
        return BookDetailResponse.CategorySummary.builder()
                .categoryId(category.getId())
                .name(category.getCategoryName())
                .slug(category.getCategorySlug())
                .build();
    }

    @Named("mapPublisherSummary")
    default BookDetailResponse.PublisherSummary mapPublisherSummary(PublisherEntity publisher) {
        if (publisher == null) return null;
        return BookDetailResponse.PublisherSummary.builder()
                .publisherId(publisher.getId())
                .publisherName(publisher.getPublisherName())
                .publisherCode(publisher.getPublisherCode())
                .build();
    }

    @Named("mapBookAuthorsToCardAuthors")
    default List<BookCatalogCardDto.AuthorSummary> mapBookAuthorsToCardAuthors(List<BookAuthorEntity> bookAuthors) {
        if (bookAuthors == null || bookAuthors.isEmpty()) return Collections.emptyList();
        return bookAuthors.stream()
                .map(ba -> BookCatalogCardDto.AuthorSummary.builder()
                        .authorId(ba.getAuthor().getId())
                        .name(ba.getAuthor().getFullName())
                        .role(ba.getContributionRole())
                        .build())
                .toList();
    }

    @Named("mapBookAuthorsToDetailAuthors")
    default List<BookDetailResponse.AuthorSummary> mapBookAuthorsToDetailAuthors(List<BookAuthorEntity> bookAuthors) {
        if (bookAuthors == null || bookAuthors.isEmpty()) return Collections.emptyList();
        return bookAuthors.stream()
                .map(ba -> BookDetailResponse.AuthorSummary.builder()
                        .authorId(ba.getAuthor().getId())
                        .name(ba.getAuthor().getFullName())
                        .authorSlug(ba.getAuthor().getAuthorSlug())
                        .role(ba.getContributionRole())
                        .build())
                .toList();
    }

    @Named("mapFormatsToCardFormats")
    default List<BookCatalogCardDto.BookFormatSummary> mapFormatsToCardFormats(List<BookFormatEntity> formats) {
        if (formats == null || formats.isEmpty()) return Collections.emptyList();
        return formats.stream()
                .map(f -> BookCatalogCardDto.BookFormatSummary.builder()
                        .formatId(f.getId())
                        .formatType(f.getFormatType())
                        .sku(f.getSku())
                        .price(MoneyDto.of(f.getBasePriceAmount(), f.getCurrencyCode()))
                        .inStock(f.getInventoryQuantity() > 0)
                        .build())
                .toList();
    }

    @Named("mapFormatsToDetailFormats")
    default List<BookDetailResponse.BookFormatDetail> mapFormatsToDetailFormats(List<BookFormatEntity> formats) {
        if (formats == null || formats.isEmpty()) return Collections.emptyList();
        return formats.stream()
                .map(f -> BookDetailResponse.BookFormatDetail.builder()
                        .formatId(f.getId())
                        .formatType(f.getFormatType())
                        .sku(f.getSku())
                        .basePrice(MoneyDto.of(f.getBasePriceAmount(), f.getCurrencyCode()))
                        .inventoryQuantity(f.getInventoryQuantity())
                        .inStock(f.getInventoryQuantity() > 0)
                        .weightGrams(f.getWeightGrams())
                        .build())
                .toList();
    }
}
