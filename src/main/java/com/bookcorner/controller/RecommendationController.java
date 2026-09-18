package com.bookcorner.controller;

import com.bookcorner.dto.recommendation.CrossSellResponse;
import com.bookcorner.dto.recommendation.HomeRecommendationsResponse;
import com.bookcorner.dto.recommendation.UpSellResponse;
import com.bookcorner.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Recommendations, Up-Sell, and Cross-Sell REST Controller.
 */
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recommendations", description = "Personalized carousels, automated Up-Sell collector editions, and Cross-Sell companion titles.")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "Get personalized home carousel", operationId = "getHomeRecommendations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations computed successfully.")
    })
    @GetMapping("/home")
    public ResponseEntity<HomeRecommendationsResponse> getHomeRecommendations(
            @Parameter(description = "Store code header")
            @RequestHeader(value = "X-Store-Code", required = false) String storeCode,
            @Parameter(description = "Number of items to return (max 30)")
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        log.info("GET /recommendations/home: storeCode={}, limit={}", storeCode, limit);
        HomeRecommendationsResponse response = recommendationService.getHomeRecommendations(null, limit);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Up-Sell editions for a book", operationId = "getBookUpSell")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Up-sell options computed."),
            @ApiResponse(responseCode = "404", description = "Book not found.")
    })
    @GetMapping("/books/{bookId}/upsell")
    public ResponseEntity<UpSellResponse> getBookUpSell(
            @Parameter(description = "Book ID", required = true)
            @PathVariable("bookId") UUID bookId
    ) {
        log.info("GET /recommendations/books/{}/upsell", bookId);
        UpSellResponse response = recommendationService.getBookUpSell(bookId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Cross-Sell companion titles", operationId = "getBookCrossSell")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cross-sell titles computed."),
            @ApiResponse(responseCode = "404", description = "Book not found.")
    })
    @GetMapping("/books/{bookId}/cross-sell")
    public ResponseEntity<CrossSellResponse> getBookCrossSell(
            @Parameter(description = "Book ID", required = true)
            @PathVariable("bookId") UUID bookId,
            @Parameter(description = "Maximum number of companion items")
            @RequestParam(value = "limit", defaultValue = "3") int limit
    ) {
        log.info("GET /recommendations/books/{}/cross-sell: limit={}", bookId, limit);
        CrossSellResponse response = recommendationService.getBookCrossSell(bookId, limit);
        return ResponseEntity.ok(response);
    }
}
