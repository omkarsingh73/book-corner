package com.bookcorner.service;

import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.catalog.AuthorDto;
import com.bookcorner.dto.catalog.BookCatalogCardDto;
import com.bookcorner.dto.catalog.BookDetailResponse;
import com.bookcorner.dto.catalog.CategoryNodeDto;
import com.bookcorner.entity.catalog.AuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.CategoryEntity;
import com.bookcorner.mapper.CatalogMapper;
import com.bookcorner.repository.catalog.AuthorRepository;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.catalog.BookSpecification;
import com.bookcorner.repository.catalog.CategoryRepository;
import com.bookcorner.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Catalog Query and Browsing Service.
 * Coordinates book discovery, multi-faceted filtering, full-text search,
 * category hierarchical navigation, and author profile lookups.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final ReviewRepository reviewRepository;
    private final CatalogMapper catalogMapper;

    /**
     * Discovers and filters catalog books using dynamic multi-attribute specifications.
     */
    @Transactional(readOnly = true)
    public Page<BookCatalogCardDto> browseBooks(
            UUID categoryId,
            String authorSlug,
            String formatType,
            Long minPrice,
            Long maxPrice,
            String language,
            Pageable pageable
    ) {
        log.info("Browsing catalog: categoryId={}, authorSlug={}, format={}, priceRange=[{}-{}], lang={}, page={}",
                categoryId, authorSlug, formatType, minPrice, maxPrice, language, pageable.getPageNumber());

        Specification<BookEntity> spec = BookSpecification.withFilters(
                categoryId, authorSlug, formatType, minPrice, maxPrice, language
        );

        Page<BookEntity> bookPage = bookRepository.findAll(spec, pageable);
        return bookPage.map(catalogMapper::toBookCatalogCardDto);
    }

    /**
     * Retrieves detailed book specification, editorial metadata, formats, and rating summary.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "books", key = "#bookId")
    public BookDetailResponse getBookDetails(UUID bookId) {
        log.info("Fetching complete book details for ID: {}", bookId);

        BookEntity book = bookRepository.findByIdWithDetails(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        Double avgRating = 0.0;
        Long reviewCount = 0L;
        try {
            Double avg = reviewRepository.findAverageRatingByBookId(bookId);
            Long count = reviewRepository.countByBookId(bookId);
            if (avg != null) avgRating = avg;
            if (count != null) reviewCount = count;
        } catch (Exception e) {
            log.warn("Unable to fetch aggregate rating metrics for book {}: {}", bookId, e.getMessage());
        }

        return catalogMapper.toBookDetailResponse(book, avgRating, reviewCount);
    }

    /**
     * Retrieves book details by unique ISBN-13.
     */
    @Transactional(readOnly = true)
    public BookDetailResponse getBookByIsbn(String isbn) {
        log.info("Fetching complete book details for ISBN: {}", isbn);
        BookEntity book = bookRepository.findByIsbn13(isbn)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ISBN: " + isbn));

        Double avgRating = 0.0;
        Long reviewCount = 0L;
        try {
            Double avg = reviewRepository.findAverageRatingByBookId(book.getId());
            Long count = reviewRepository.countByBookId(book.getId());
            if (avg != null) avgRating = avg;
            if (count != null) reviewCount = count;
        } catch (Exception e) {
            log.warn("Unable to fetch aggregate rating metrics for book {}: {}", book.getId(), e.getMessage());
        }

        return catalogMapper.toBookDetailResponse(book, avgRating, reviewCount);
    }

    /**
     * Executes natural language full-text search against book titles, subtitles, and synopses.
     */
    @Transactional(readOnly = true)
    public Page<BookCatalogCardDto> searchCatalog(String query, Pageable pageable) {
        if (query == null || query.trim().isBlank()) {
            log.info("Empty search query provided, delegating to default catalog browsing.");
            return browseBooks(null, null, null, null, null, null, pageable);
        }

        String sanitizedQuery = query.trim();
        log.info("Executing full-text search for keyword: '{}', page: {}", sanitizedQuery, pageable.getPageNumber());

        Page<BookEntity> results = bookRepository.searchByFullText(sanitizedQuery, pageable);
        return results.map(catalogMapper::toBookCatalogCardDto);
    }

    /**
     * Retrieves top-level category tree with subcategories eagerly populated.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", unless = "#result == null || #result.isEmpty()")
    public List<CategoryNodeDto> listCategories() {
        log.info("Fetching complete category hierarchical tree");
        List<CategoryEntity> rootCategories = categoryRepository.findAllRootCategoriesWithSubcategories();
        return catalogMapper.toCategoryNodeDtoList(rootCategories);
    }

    /**
     * Searches authors by name substring with pagination.
     */
    @Transactional(readOnly = true)
    public Page<AuthorDto> listAuthors(String name, Pageable pageable) {
        log.info("Querying authors: query='{}', page={}", name, pageable.getPageNumber());

        Page<AuthorEntity> authors;
        if (name != null && !name.trim().isBlank()) {
            authors = authorRepository.findByFullNameContainingIgnoreCase(name.trim(), pageable);
        } else {
            authors = authorRepository.findAll(pageable);
        }

        return authors.map(catalogMapper::toAuthorDto);
    }

    /**
     * Retrieves author biography and details by ID.
     */
    @Transactional(readOnly = true)
    public AuthorDto getAuthorDetails(UUID authorId) {
        log.info("Fetching author profile for ID: {}", authorId);
        AuthorEntity author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found with ID: " + authorId));
        return catalogMapper.toAuthorDto(author);
    }

    /**
     * Retrieves author biography and details by unique slug.
     */
    @Transactional(readOnly = true)
    public AuthorDto getAuthorBySlug(String authorSlug) {
        log.info("Fetching author profile for slug: {}", authorSlug);
        AuthorEntity author = authorRepository.findByAuthorSlug(authorSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found for slug: " + authorSlug));
        return catalogMapper.toAuthorDto(author);
    }
}
