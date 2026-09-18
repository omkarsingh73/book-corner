package com.bookcorner.service;

import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.catalog.BookCatalogCardDto;
import com.bookcorner.dto.catalog.BookDetailResponse;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.CategoryEntity;
import com.bookcorner.entity.catalog.PublisherEntity;
import com.bookcorner.mapper.CatalogMapper;
import com.bookcorner.repository.catalog.AuthorRepository;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.catalog.CategoryRepository;
import com.bookcorner.repository.review.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Unit Tests (Mockito & AssertJ)")
class CatalogServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CatalogMapper catalogMapper;

    @InjectMocks
    private CatalogService catalogService;

    private UUID bookId;
    private BookEntity book;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();

        PublisherEntity publisher = PublisherEntity.builder()
                .id(UUID.randomUUID())
                .publisherName("Pearson")
                .build();

        CategoryEntity category = CategoryEntity.builder()
                .id(UUID.randomUUID())
                .categoryName("Computer Science")
                .build();

        book = BookEntity.builder()
                .id(bookId)
                .isbn13("9780132350884")
                .title("Clean Code")
                .subtitle("A Handbook of Agile Software Craftsmanship")
                .publisher(publisher)
                .primaryCategory(category)
                .language("en")
                .publicationDate(LocalDate.of(2008, 8, 1))
                .formats(new ArrayList<>())
                .bookAuthors(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should retrieve book details by ID")
    void shouldGetBookDetailsById() {
        BookDetailResponse expectedDto = BookDetailResponse.builder()
                .id(bookId)
                .title("Clean Code")
                .isbn13("9780132350884")
                .build();

        when(bookRepository.findByIdWithDetails(bookId)).thenReturn(Optional.of(book));
        when(reviewRepository.findAverageRatingByBookId(bookId)).thenReturn(4.8);
        when(reviewRepository.countByBookId(bookId)).thenReturn(120L);
        when(catalogMapper.toBookDetailResponse(book, 4.8, 120L)).thenReturn(expectedDto);

        BookDetailResponse result = catalogService.getBookDetails(bookId);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getIsbn13()).isEqualTo("9780132350884");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when book does not exist")
    void shouldThrowResourceNotFoundExceptionForMissingBook() {
        UUID missingId = UUID.randomUUID();
        when(bookRepository.findByIdWithDetails(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getBookDetails(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("Should retrieve book by ISBN-13")
    void shouldGetBookByIsbn() {
        BookDetailResponse expectedDto = BookDetailResponse.builder()
                .id(bookId)
                .title("Clean Code")
                .isbn13("9780132350884")
                .build();

        when(bookRepository.findByIsbn13("9780132350884")).thenReturn(Optional.of(book));
        when(reviewRepository.findAverageRatingByBookId(bookId)).thenReturn(4.8);
        when(reviewRepository.countByBookId(bookId)).thenReturn(120L);
        when(catalogMapper.toBookDetailResponse(book, 4.8, 120L)).thenReturn(expectedDto);

        BookDetailResponse result = catalogService.getBookByIsbn("9780132350884");

        assertThat(result).isNotNull();
        assertThat(result.getIsbn13()).isEqualTo("9780132350884");
    }

    @Test
    @DisplayName("Should browse books with specification filter and pagination")
    void shouldBrowseBooksWithPagination() {
        Page<BookEntity> page = new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1);
        BookCatalogCardDto cardDto = BookCatalogCardDto.builder()
                .id(bookId)
                .title("Clean Code")
                .build();

        when(bookRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 10)))).thenReturn(page);
        when(catalogMapper.toBookCatalogCardDto(book)).thenReturn(cardDto);

        Page<BookCatalogCardDto> result = catalogService.browseBooks(
                null, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
    }
}
