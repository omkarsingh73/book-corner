package com.bookcorner.mapper;

import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.dto.payment.CustomerWalletResponse;
import com.bookcorner.dto.payment.PaymentIntentResponse;
import com.bookcorner.entity.payment.CustomerWalletEntity;
import com.bookcorner.entity.payment.PaymentTransactionEntity;
import com.bookcorner.entity.payment.TenderSplitEntity;
import com.bookcorner.entity.payment.WalletLedgerEntryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for Payment transactions, tender splits, and customer digital wallets.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface PaymentMapper {

    // --- Payment Intent Mapping ---
    @Mapping(target = "paymentTransactionId", source = "id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "totalPayableAmount", source = "entity", qualifiedByName = "mapTxTotalPayable")
    @Mapping(target = "clientSecret", source = "gatewayTransactionId")
    @Mapping(target = "splits", source = "tenderSplits")
    PaymentIntentResponse toPaymentIntentResponse(PaymentTransactionEntity entity);

    @Mapping(target = "status", source = "tenderStatus")
    @Mapping(target = "amount", source = "entity", qualifiedByName = "mapSplitAmount")
    PaymentIntentResponse.TenderSplitSummary toTenderSplitSummary(TenderSplitEntity entity);

    // --- Customer Wallet Mapping ---
    @Mapping(target = "walletId", source = "id")
    @Mapping(target = "currentBalance", source = "entity", qualifiedByName = "mapWalletBalance")
    @Mapping(target = "recentEntries", source = "ledgerEntries")
    CustomerWalletResponse toCustomerWalletResponse(CustomerWalletEntity entity);

    @Mapping(target = "amount", source = "entity", qualifiedByName = "mapLedgerAmount")
    @Mapping(target = "balanceAfter", source = "entity", qualifiedByName = "mapLedgerBalanceAfter")
    @Mapping(target = "timestamp", source = "createdAt")
    CustomerWalletResponse.LedgerEntrySummary toLedgerEntrySummary(WalletLedgerEntryEntity entity);

    // --- Named Helpers ---
    @Named("mapTxTotalPayable")
    default MoneyDto mapTxTotalPayable(PaymentTransactionEntity tx) {
        if (tx == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(tx.getTotalPayableAmount(), tx.getCurrencyCode());
    }

    @Named("mapSplitAmount")
    default MoneyDto mapSplitAmount(TenderSplitEntity split) {
        if (split == null || split.getPaymentTransaction() == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(split.getAmount(), split.getPaymentTransaction().getCurrencyCode());
    }

    @Named("mapWalletBalance")
    default MoneyDto mapWalletBalance(CustomerWalletEntity wallet) {
        if (wallet == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(wallet.getCurrentBalance(), wallet.getCurrencyCode());
    }

    @Named("mapLedgerAmount")
    default MoneyDto mapLedgerAmount(WalletLedgerEntryEntity entry) {
        if (entry == null || entry.getWallet() == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(entry.getAmount(), entry.getWallet().getCurrencyCode());
    }

    @Named("mapLedgerBalanceAfter")
    default MoneyDto mapLedgerBalanceAfter(WalletLedgerEntryEntity entry) {
        if (entry == null || entry.getWallet() == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(entry.getBalanceAfter(), entry.getWallet().getCurrencyCode());
    }
}
