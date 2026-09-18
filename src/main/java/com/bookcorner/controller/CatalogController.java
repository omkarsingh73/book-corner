package com.bookcorner.controller;

import com.bookcorner.dto.catalog.AuthorDto;
import com.bookcorner.dto.catalog.BookCatalogCardDto;
import com.bookcorner.dto.catalog.BookDetailResponse;
import com.bookcorner.dto.catalog.CategoryNodeDto;
import com.bookcorner.dto.catalog.PublisherDto;
import com.bookcorner.mapper.CatalogMapper;
import com.bookcorner.repository.catalog.PublisherRepository;
import com.bookcorner.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Product Master and Catalog Browsing REST Controller.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Catalog", description = "Product master books, format SKUs, publishers, authors, and hierarchical categories.")
public class CatalogController {

    private final CatalogService catalogService;
    private final PublisherRepository publisherRepository;
    private final CatalogMapper catalogMapper;

    @Operation(summary = "List hierarchical categories", operationId = "listCategories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories hierarchy tree retrieved.")
    })
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryNodeDto>> listCategories() {
        log.info("GET /categories");
        List<CategoryNodeDto> categories = catalogService.listCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Browse authors", operationId = "listAuthors")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authors list retrieved.")
    })
    @GetMapping("/authors")
    public ResponseEntity<Page<AuthorDto>> listAuthors(
            @Parameter(description = "Optional author name search filter")
            @RequestParam(value = "name", required = false) String name,
            @PageableDefault(page = 0, size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("GET /authors: name={}, page={}", name, pageable.getPageNumber());
        Page<AuthorDto> authors = catalogService.listAuthors(name, pageable);
        return ResponseEntity.ok(authors);
    }

    @Operation(summary = "Get author profile by ID", operationId = "getAuthorById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author details retrieved."),
            @ApiResponse(responseCode = "404", description = "Author not found.")
    })
    @GetMapping("/authors/{authorId}")
    public ResponseEntity<AuthorDto> getAuthorById(
            @Parameter(description = "Author ID", required = true)
            @PathVariable("authorId") UUID authorId
    ) {
        log.info("GET /authors/{}", authorId);
        AuthorDto author = catalogService.getAuthorDetails(authorId);
        return ResponseEntity.ok(author);
    }

    @Operation(summary = "List publishers", operationId = "listPublishers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publishers list retrieved.")
    })
    @GetMapping("/publishers")
    public ResponseEntity<Page<PublisherDto>> listPublishers(
            @PageableDefault(page = 0, size = 20, sort = "publisherName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("GET /publishers: page={}", pageable.getPageNumber());
        var publishers = publisherRepository.findAll(pageable).map(catalogMapper::toPublisherDto);
        return ResponseEntity.ok(publishers);
    }

    @Operation(summary = "Browse catalog books with faceted filters", operationId = "browseBooks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameter.")
    })
    @GetMapping("/books")
    public ResponseEntity<Page<BookCatalogCardDto>> browseBooks(
            @Parameter(description = "Filter by primary category ID")
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @Parameter(description = "Filter by author URL slug")
            @RequestParam(value = "authorSlug", required = false) String authorSlug,
            @Parameter(description = "Filter by format (PAPERBACK, HARDCOVER, EBOOK, AUDIOBOOK)")
            @RequestParam(value = "formatType", required = false) String formatType,
            @Parameter(description = "Minimum price in cents")
            @RequestParam(value = "minPrice", required = false) Long minPrice,
            @Parameter(description = "Maximum price in cents")
            @RequestParam(value = "maxPrice", required = false) Long maxPrice,
            @Parameter(description = "Primary language code (e.g. en, es, fr)")
            @RequestParam(value = "language", required = false) String language,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("GET /books: category={}, author={}, format={}, priceRange=[{}-{}], lang={}",
                categoryId, authorSlug, formatType, minPrice, maxPrice, language);

        Page<BookCatalogCardDto> books = catalogService.browseBooks(
                categoryId, authorSlug, formatType, minPrice, maxPrice, language, pageable
        );
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Get complete book details by ID", operationId = "getBookById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book details retrieved."),
            @ApiResponse(responseCode = "404", description = "Book not found.")
    })
    @GetMapping("/books/{bookId}")
    public ResponseEntity<BookDetailResponse> getBookById(
            @Parameter(description = "Book ID", required = true)
            @PathVariable("bookId") UUID bookId
    ) {
        log.info("GET /books/{}", bookId);
        BookDetailResponse book = catalogService.getBookDetails(bookId);
        return ResponseEntity.ok(book);
    }
}
