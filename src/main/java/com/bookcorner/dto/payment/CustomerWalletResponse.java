package com.bookcorner.dto.payment;

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
 * Customer digital wallet balance and recent ledger activity response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerWalletResponse implements Serializable {

    private UUID walletId;
    private MoneyDto currentBalance;
    private String currencyCode;
    private Boolean isLocked;

    public String getCurrencyCode() {
        if (currencyCode != null) {
            return currencyCode;
        }
        return currentBalance != null ? currentBalance.getCurrency() : null;
    }

    @Builder.Default
    private List<LedgerEntrySummary> recentEntries = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LedgerEntrySummary implements Serializable {
        private String entryType;
        private MoneyDto amount;
        private MoneyDto balanceAfter;
        private String referenceId;
        private String notes;
        private Instant timestamp;
    }
}
