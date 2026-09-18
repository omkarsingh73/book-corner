package com.bookcorner.repository;

import com.bookcorner.config.AbstractPostgresRepositoryTest;
import com.bookcorner.entity.catalog.AuthorEntity;
import com.bookcorner.entity.catalog.BookAuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.catalog.CategoryEntity;
import com.bookcorner.entity.catalog.PublisherEntity;
import com.bookcorner.repository.catalog.AuthorRepository;
import com.bookcorner.repository.catalog.BookFormatRepository;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.catalog.CategoryRepository;
import com.bookcorner.repository.catalog.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookRepository Integration Tests (PostgreSQL Testcontainers)")
class BookRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookFormatRepository bookFormatRepository;

    private PublisherEntity testPublisher;
    private CategoryEntity testCategory;
    private AuthorEntity testAuthor;
    private BookEntity testBook;

    @BeforeEach
    void setUp() {
        bookFormatRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        categoryRepository.deleteAll();
        publisherRepository.deleteAll();

        testPublisher = publisherRepository.save(PublisherEntity.builder()
                .publisherName("O'Reilly Media")
                .publisherCode("OREILLY")
                .contactEmail("orders@oreilly.com")
                .build());

        testCategory = categoryRepository.save(CategoryEntity.builder()
                .categoryName("Computer Science")
                .categorySlug("computer-science")
                .treeLevel(0)
                .displayOrder(1)
                .build());

        testAuthor = authorRepository.save(AuthorEntity.builder()
                .fullName("Martin Kleppmann")
                .authorSlug("martin-kleppmann")
                .biography("Distributed systems researcher")
                .build());

        testBook = BookEntity.builder()
                .isbn13("9781449373320")
                .isbn10("1449373321")
                .title("Designing Data-Intensive Applications")
                .subtitle("The Big Ideas Behind Reliable, Scalable, and Maintainable Systems")
                .publisher(testPublisher)
                .primaryCategory(testCategory)
                .language("en")
                .publicationDate(LocalDate.of(2017, 3, 16))
                .pageCount(616)
                .synopsis("Comprehensive guide to data architecture.")
                .formats(new ArrayList<>())
                .bookAuthors(new ArrayList<>())
                .build();

        testBook = bookRepository.save(testBook);

        BookAuthorEntity bookAuthor = BookAuthorEntity.builder()
                .id(new BookAuthorEntity.BookAuthorId(testBook.getId(), testAuthor.getId()))
                .book(testBook)
                .author(testAuthor)
                .contributionRole("AUTHOR")
                .authorSequence(1)
                .build();
        testBook.getBookAuthors().add(bookAuthor);
        bookRepository.save(testBook);

        BookFormatEntity format = bookFormatRepository.save(BookFormatEntity.builder()
                .book(testBook)
                .sku("SKU-DDIA-PB")
                .formatType("PAPERBACK")
                .basePriceAmount(4499L)
                .currencyCode("USD")
                .stockQuantity(150)
                .weightGrams(850)
                .build());
        testBook.getFormats().add(format);
    }

    @Test
    @DisplayName("Should find book by ISBN-13 with eagerly loaded publisher and category")
    void shouldFindBookByIsbn13() {
        Optional<BookEntity> found = bookRepository.findByIsbn13("9781449373320");

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Designing Data-Intensive Applications");
        assertThat(found.get().getPublisher().getPublisherCode()).isEqualTo("OREILLY");
        assertThat(found.get().getPrimaryCategory().getCategorySlug()).isEqualTo("computer-science");
    }

    @Test
    @DisplayName("Should return true when ISBN-13 exists")
    void shouldCheckIsbnExistence() {
        boolean exists = bookRepository.existsByIsbn13("9781449373320");
        boolean notExists = bookRepository.existsByIsbn13("9780000000000");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find books by primary category ID with pagination")
    void shouldFindByPrimaryCategoryId() {
        Page<BookEntity> page = bookRepository.findByPrimaryCategoryId(testCategory.getId(), PageRequest.of(0, 10));

        assertThat(page).isNotEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Designing Data-Intensive Applications");
    }

    @Test
    @DisplayName("Should find books by author ID through junction join")
    void shouldFindByAuthorId() {
        Page<BookEntity> page = bookRepository.findByAuthorId(testAuthor.getId(), PageRequest.of(0, 10));

        assertThat(page).isNotEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getIsbn13()).isEqualTo("9781449373320");
    }

    @Test
    @DisplayName("Should retrieve full book details graph including formats and authors")
    void shouldFindByIdWithDetails() {
        Optional<BookEntity> details = bookRepository.findByIdWithDetails(testBook.getId());

        assertThat(details).isPresent();
        assertThat(details.get().getPublisher()).isNotNull();
        assertThat(details.get().getPrimaryCategory()).isNotNull();
        assertThat(details.get().getFormats()).isNotEmpty();
        assertThat(details.get().getFormats().get(0).getSku()).isEqualTo("SKU-DDIA-PB");
    }
}
