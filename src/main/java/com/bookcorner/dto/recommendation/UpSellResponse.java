package com.bookcorner.dto.recommendation;

import com.bookcorner.dto.common.MoneyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Premium format Up-Sell options for a book matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpSellResponse implements Serializable {

    private UUID sourceBookId;
    private Boolean hasUpSell;
    private UpSellOption upSellOption;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpSellOption implements Serializable {
        private UUID targetBookId;
        private String formatType;
        private String editionTitle;
        private MoneyDto basePrice;
        private MoneyDto priceDelta;

        @Builder.Default
        private List<String> benefits = new ArrayList<>();
    }
}
