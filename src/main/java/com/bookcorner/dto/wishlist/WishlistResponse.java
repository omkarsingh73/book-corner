package com.bookcorner.dto.wishlist;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Customer wishlist container response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse implements Serializable {

    private UUID wishlistId;
    private String name;
    private Integer totalItems;

    @Builder.Default
    private List<WishlistItemDto> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistItemDto implements Serializable {
        private UUID bookId;
        private String title;
        private String authorNames;
        private String coverImageUrl;
        private String primaryFormat;
        private MoneyDto currentPrice;
        private MoneyDto desiredPriceAlert;
        private Instant addedAt;
    }
}
