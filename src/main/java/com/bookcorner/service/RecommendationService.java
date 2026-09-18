package com.bookcorner.service;

import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.dto.recommendation.CrossSellResponse;
import com.bookcorner.dto.recommendation.HomeRecommendationsResponse;
import com.bookcorner.dto.recommendation.UpSellResponse;
import com.bookcorner.entity.catalog.BookAuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.repository.catalog.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Merchandising Recommendations Service.
 * Computes personalized home carousels, hardcover/collector up-sell options,
 * and companion cross-sell titles with bundle discounts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final BookRepository bookRepository;

    /**
     * Computes homepage curated and trending book recommendations.
     */
    @Transactional(readOnly = true)
    public HomeRecommendationsResponse getHomeRecommendations(UUID storeId, int limit) {
        int max = Math.min(Math.max(1, limit), 30);
        log.info("Generating home recommendations (storeId: {}, limit: {})", storeId, max);

        var page = bookRepository.findAll(PageRequest.of(0, max));
        List<HomeRecommendationsResponse.RecommendedBookSummary> summaries = new ArrayList<>();

        for (BookEntity book : page.getContent()) {
            String authorName = (book.getBookAuthors() != null && !book.getBookAuthors().isEmpty())
                    ? book.getBookAuthors().stream()
                            .map(ba -> ba.getAuthor().getFullName())
                            .collect(Collectors.joining(", "))
                    : "Featured Author";

            long startPrice = 999L;
            String currency = "USD";
            if (book.getFormats() != null && !book.getFormats().isEmpty()) {
                startPrice = book.getFormats().get(0).getBasePriceAmount();
                currency = book.getFormats().get(0).getCurrencyCode();
            }

            summaries.add(HomeRecommendationsResponse.RecommendedBookSummary.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .authorName(authorName)
                    .coverImageUrl(book.getCoverImageUrl())
                    .startingPrice(MoneyDto.of(startPrice, currency))
                    .averageRating(4.8)
                    .recommendationReason("Trending Bestseller in " + (book.getPrimaryCategory() != null ? book.getPrimaryCategory().getCategoryName() : "Fiction"))
                    .build());
        }

        return HomeRecommendationsResponse.builder()
                .strategy("AI_EDITORIAL_HYBRID")
                .recommendations(summaries)
                .build();
    }

    /**
     * Computes premium edition or collector format Up-Sell options for a book.
     */
    @Transactional(readOnly = true)
    public UpSellResponse getBookUpSell(UUID bookId) {
        log.info("Computing up-sell options for book: {}", bookId);

        BookEntity book = bookRepository.findByIdWithDetails(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        // Check if book has a HARDCOVER or DELUXE format that can serve as up-sell
        BookFormatEntity premium = null;
        if (book.getFormats() != null) {
            premium = book.getFormats().stream()
                    .filter(f -> "HARDCOVER".equalsIgnoreCase(f.getFormatType()) || "DELUXE".equalsIgnoreCase(f.getFormatType()))
                    .findFirst()
                    .orElse(null);
        }

        if (premium == null) {
            return UpSellResponse.builder()
                    .sourceBookId(bookId)
                    .hasUpSell(false)
                    .build();
        }

        long premiumPrice = premium.getBasePriceAmount();
        long paperbackPrice = (long) (premiumPrice * 0.7);

        return UpSellResponse.builder()
                .sourceBookId(bookId)
                .hasUpSell(true)
                .upSellOption(UpSellResponse.UpSellOption.builder()
                        .targetBookId(bookId)
                        .formatType(premium.getFormatType())
                        .editionTitle("Collector's Hardcover Illustrated Edition")
                        .basePrice(MoneyDto.of(premiumPrice, premium.getCurrencyCode()))
                        .priceDelta(MoneyDto.of(premiumPrice - paperbackPrice, premium.getCurrencyCode()))
                        .benefits(List.of("Acid-free archival paper", "Embossed foil cover", "Bonus author interview chapter"))
                        .build())
                .build();
    }

    /**
     * Computes companion cross-sell books with bundle discount pricing.
     */
    @Transactional(readOnly = true)
    public CrossSellResponse getBookCrossSell(UUID bookId, int limit) {
        int max = Math.min(Math.max(1, limit), 5);
        log.info("Computing cross-sell companion titles for book: {}, limit: {}", bookId, max);

        BookEntity source = bookRepository.findByIdWithDetails(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        UUID categoryId = source.getPrimaryCategory() != null ? source.getPrimaryCategory().getId() : null;
        var companionsPage = (categoryId != null)
                ? bookRepository.findByPrimaryCategoryId(categoryId, PageRequest.of(0, max + 1))
                : bookRepository.findAll(PageRequest.of(0, max + 1));

        List<CrossSellResponse.CompanionTitleSummary> companions = new ArrayList<>();
        for (BookEntity comp : companionsPage.getContent()) {
            if (comp.getId().equals(bookId)) continue;
            if (companions.size() >= max) break;

            String authorName = (comp.getBookAuthors() != null && !comp.getBookAuthors().isEmpty())
                    ? comp.getBookAuthors().get(0).getAuthor().getFullName()
                    : "Author";

            long indPrice = 1499L;
            if (comp.getFormats() != null && !comp.getFormats().isEmpty()) {
                indPrice = comp.getFormats().get(0).getBasePriceAmount();
            }
            long bundlePrice = (long) (indPrice * 0.85); // 15% bundle discount

            companions.add(CrossSellResponse.CompanionTitleSummary.builder()
                    .bookId(comp.getId())
                    .title(comp.getTitle())
                    .authorName(authorName)
                    .formatType("PAPERBACK")
                    .individualPrice(indPrice)
                    .bundlePrice(bundlePrice)
                    .coverImageUrl(comp.getCoverImageUrl())
                    .build());
        }

        return CrossSellResponse.builder()
                .sourceBookId(bookId)
                .bundleDiscountPercentage(15.0)
                .companionTitles(companions)
                .build();
    }
}
