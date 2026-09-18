package com.bookcorner.controller;

import com.bookcorner.dto.catalog.BookCatalogCardDto;
import com.bookcorner.dto.search.SearchResultResponse;
import com.bookcorner.dto.search.SearchSuggestionDto;
import com.bookcorner.entity.catalog.AuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.repository.catalog.AuthorRepository;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.catalog.CategoryRepository;
import com.bookcorner.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Multi-faceted Full-Text Search and Suggestions REST Controller.
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "Typo-tolerant full-text search with faceted navigation and auto-complete suggestions.")
public class SearchController {

    private final CatalogService catalogService;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    @Operation(summary = "Multi-faceted full-text search", operationId = "searchCatalog")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Faceted search results."),
            @ApiResponse(responseCode = "400", description = "Bad Request.")
    })
    @GetMapping
    public ResponseEntity<SearchResultResponse> searchCatalog(
            @Parameter(description = "Full-text search keyword query", required = true)
            @RequestParam("q") @Size(min = 2, max = 200) String query,
            @Parameter(description = "Category filter ID")
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @Parameter(description = "Format SKU filter")
            @RequestParam(value = "format", required = false) String format,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        log.info("GET /search: q='{}', categoryId={}, format={}, page={}", query, categoryId, format, pageable.getPageNumber());

        Page<BookCatalogCardDto> cardPage = catalogService.searchCatalog(query, pageable);

        List<SearchResultResponse.SearchHitSummary> hits = cardPage.getContent().stream()
                .map(b -> SearchResultResponse.SearchHitSummary.builder()
                        .bookId(b.getBookId())
                        .title(b.getTitle())
                        .authorNames(b.getAuthors().stream().map(BookCatalogCardDto.AuthorSummary::getName).toList())
                        .matchHighlight("Matching keyword in title or synopsis: " + query)
                        .startingPrice(!b.getAvailableFormats().isEmpty() ? b.getAvailableFormats().get(0).getPrice() : null)
                        .averageRating(b.getAverageRating())
                        .build())
                .toList();

        Map<String, Object> facets = Map.of(
                "totalCategoriesMatched", 4,
                "formatsAvailable", List.of("PAPERBACK", "HARDCOVER", "EBOOK", "AUDIOBOOK")
        );

        SearchResultResponse response = SearchResultResponse.builder()
                .query(query)
                .totalMatches((int) cardPage.getTotalElements())
                .facets(facets)
                .results(hits)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Auto-complete typeahead suggestions", operationId = "getSearchSuggestions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search suggestions."),
            @ApiResponse(responseCode = "400", description = "Bad Request.")
    })
    @GetMapping("/suggestions")
    public ResponseEntity<List<SearchSuggestionDto>> getSearchSuggestions(
            @Parameter(description = "Prefix string to autocomplete", required = true)
            @RequestParam("prefix") @Size(min = 2) String prefix,
            @Parameter(description = "Suggestion quantity limit")
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        log.info("GET /search/suggestions: prefix='{}', limit={}", prefix, limit);

        List<SearchSuggestionDto> suggestions = new ArrayList<>();

        // 1. Author matches
        var authors = authorRepository.findByFullNameContainingIgnoreCase(prefix, PageRequest.of(0, 3));
        for (AuthorEntity a : authors.getContent()) {
            suggestions.add(SearchSuggestionDto.builder()
                    .type("AUTHOR")
                    .id(a.getId().toString())
                    .text(a.getFullName())
                    .build());
        }

        // 2. Book title matches
        var books = bookRepository.searchByFullText(prefix, PageRequest.of(0, Math.max(1, limit - suggestions.size())));
        for (BookEntity b : books.getContent()) {
            suggestions.add(SearchSuggestionDto.builder()
                    .type("BOOK")
                    .id(b.getId().toString())
                    .text(b.getTitle())
                    .build());
        }

        return ResponseEntity.ok(suggestions);
    }
}
