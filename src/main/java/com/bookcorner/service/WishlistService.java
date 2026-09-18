package com.bookcorner.service;

import com.bookcorner.common.exception.DuplicateResourceException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.dto.wishlist.AddWishlistItemRequest;
import com.bookcorner.dto.wishlist.WishlistResponse;
import com.bookcorner.entity.catalog.BookAuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.member.WishlistEntity;
import com.bookcorner.entity.member.WishlistItemEntity;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.member.WishlistItemRepository;
import com.bookcorner.repository.member.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Customer Wishlist and Saved Items Service.
 * Manages customer personal collections, price drop alert thresholds, and item removals.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves customer wishlist or initializes one if it does not yet exist.
     */
    @Transactional
    public WishlistResponse getWishlist(UUID userId) {
        log.info("Retrieving wishlist for user ID: {}", userId);
        WishlistEntity wishlist = resolveOrCreateWishlist(userId);
        return toWishlistResponse(wishlist);
    }

    /**
     * Adds a book to customer wishlist with an optional desired price-drop alert.
     */
    @Transactional
    public WishlistResponse addToWishlist(UUID userId, AddWishlistItemRequest request) {
        log.info("Adding book ID {} to wishlist for user ID: {}", request.getBookId(), userId);

        WishlistEntity wishlist = resolveOrCreateWishlist(userId);

        BookEntity book = bookRepository.findByIdWithDetails(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + request.getBookId()));

        boolean alreadyPresent = wishlist.getItems().stream()
                .anyMatch(i -> i.getBook().getId().equals(book.getId()));

        if (alreadyPresent) {
            log.warn("Book {} already present in wishlist {}", book.getId(), wishlist.getId());
            throw new DuplicateResourceException("This book is already in your wishlist.");
        }

        Long alertAmount = (request.getDesiredPriceAlert() != null) ? request.getDesiredPriceAlert().getAmount() : null;

        WishlistItemEntity item = WishlistItemEntity.builder()
                .wishlist(wishlist)
                .book(book)
                .desiredPriceAlert(alertAmount)
                .build();

        wishlist.addItem(item);
        WishlistEntity saved = wishlistRepository.save(wishlist);
        log.info("Book {} successfully added to wishlist {}", book.getId(), saved.getId());

        return toWishlistResponse(saved);
    }

    /**
     * Removes a book from customer wishlist.
     */
    @Transactional
    public WishlistResponse removeFromWishlist(UUID userId, UUID bookId) {
        log.info("Removing book ID {} from wishlist for user ID: {}", bookId, userId);

        WishlistEntity wishlist = resolveOrCreateWishlist(userId);

        WishlistItemEntity item = wishlist.getItems().stream()
                .filter(i -> i.getBook().getId().equals(bookId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Book is not present in your wishlist."));

        wishlist.removeItem(item);
        wishlistItemRepository.delete(item);

        WishlistEntity saved = wishlistRepository.save(wishlist);
        log.info("Book {} removed from wishlist {}", bookId, saved.getId());

        return toWishlistResponse(saved);
    }

    // --- Private Helper Methods ---

    private WishlistEntity resolveOrCreateWishlist(UUID userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

                    WishlistEntity newWishlist = WishlistEntity.builder()
                            .user(user)
                            .wishlistName("My Wishlist")
                            .isPublic(false)
                            .build();

                    log.info("Creating fresh wishlist for user {}", userId);
                    return wishlistRepository.save(newWishlist);
                });
    }

    private WishlistResponse toWishlistResponse(WishlistEntity wishlist) {
        List<WishlistResponse.WishlistItemDto> itemDtos = new ArrayList<>();

        if (wishlist.getItems() != null) {
            for (WishlistItemEntity item : wishlist.getItems()) {
                BookEntity b = item.getBook();

                String authorNames = (b.getBookAuthors() != null && !b.getBookAuthors().isEmpty())
                        ? b.getBookAuthors().stream()
                                .map(ba -> ba.getAuthor().getFullName())
                                .collect(Collectors.joining(", "))
                        : "Unknown Author";

                String primaryFormat = "PAPERBACK";
                MoneyDto currentPrice = MoneyDto.of(0L, "USD");

                if (b.getFormats() != null && !b.getFormats().isEmpty()) {
                    BookFormatEntity lowest = b.getFormats().stream()
                            .min(Comparator.comparingLong(BookFormatEntity::getBasePriceAmount))
                            .orElse(b.getFormats().get(0));
                    primaryFormat = lowest.getFormatType();
                    currentPrice = MoneyDto.of(lowest.getBasePriceAmount(), lowest.getCurrencyCode());
                }

                MoneyDto priceAlert = (item.getDesiredPriceAlert() != null)
                        ? MoneyDto.of(item.getDesiredPriceAlert(), currentPrice.getCurrency())
                        : null;

                itemDtos.add(WishlistResponse.WishlistItemDto.builder()
                        .bookId(b.getId())
                        .title(b.getTitle())
                        .authorNames(authorNames)
                        .coverImageUrl(b.getCoverImageUrl())
                        .primaryFormat(primaryFormat)
                        .currentPrice(currentPrice)
                        .desiredPriceAlert(priceAlert)
                        .addedAt(item.getCreatedAt())
                        .build());
            }
        }

        return WishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .name(wishlist.getWishlistName())
                .totalItems(itemDtos.size())
                .items(itemDtos)
                .build();
    }
}
