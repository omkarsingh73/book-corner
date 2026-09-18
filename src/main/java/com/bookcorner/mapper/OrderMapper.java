package com.bookcorner.mapper;

import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.dto.order.OrderCancellationResponse;
import com.bookcorner.dto.order.OrderConfirmationResponse;
import com.bookcorner.dto.order.OrderDetailResponse;
import com.bookcorner.dto.order.OrderSummaryDto;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.ordering.OrderLineItemEntity;
import com.bookcorner.entity.ordering.OrderStatusHistoryEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for Order aggregates, line items, and invoice responses.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface OrderMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // --- Order Summary Mapping (Dashboard) ---
    @Mapping(target = "totalAmount", source = "entity", qualifiedByName = "mapTotalAmount")
    @Mapping(target = "itemCount", source = "lineItems", qualifiedByName = "calculateItemCount")
    @Mapping(target = "placedAt", source = "placedAt")
    OrderSummaryDto toOrderSummaryDto(OrderEntity entity);

    List<OrderSummaryDto> toOrderSummaryDtoList(List<OrderEntity> entities);

    // --- Order Confirmation (Checkout Synchronous Result) ---
    @Mapping(target = "totalAmount", source = "entity", qualifiedByName = "mapTotalAmount")
    @Mapping(target = "estimatedDelivery", constant = "3-5 business days")
    OrderConfirmationResponse toOrderConfirmationResponse(OrderEntity entity);

    // --- Order Cancellation ---
    @Mapping(target = "refundInitiated", constant = "true")
    @Mapping(target = "refundAmount", source = "entity", qualifiedByName = "mapTotalAmount")
    OrderCancellationResponse toOrderCancellationResponse(OrderEntity entity);

    // --- Order Detail (Full Invoice & Timeline) ---
    @Mapping(target = "pricing", source = "entity", qualifiedByName = "mapPricingBreakdown")
    @Mapping(target = "shippingAddress", source = "shippingAddressSnapshot", qualifiedByName = "deserializeAddressJson")
    @Mapping(target = "billingAddress", source = "billingAddressSnapshot", qualifiedByName = "deserializeAddressJson")
    @Mapping(target = "lineItems", source = "lineItems")
    @Mapping(target = "timeline", source = "statusHistory")
    @Mapping(target = "isCancellable", source = "orderStatus", qualifiedByName = "checkCancellable")
    OrderDetailResponse toOrderDetailResponse(OrderEntity entity);

    // --- Line Item Mapping ---
    @Mapping(target = "lineItemId", source = "id")
    @Mapping(target = "formatId", source = "format.id")
    @Mapping(target = "bookTitle", source = "bookTitleSnapshot")
    @Mapping(target = "isbn13", source = "isbn13Snapshot")
    @Mapping(target = "formatType", source = "formatTypeSnapshot")
    @Mapping(target = "unitPrice", source = "entity", qualifiedByName = "mapLineUnitPrice")
    @Mapping(target = "lineTotal", source = "entity", qualifiedByName = "mapLineTotal")
    OrderDetailResponse.OrderLineItemDto toOrderLineItemDto(OrderLineItemEntity entity);

    // --- Status History Mapping ---
    @Mapping(target = "notes", source = "transitionNotes")
    @Mapping(target = "timestamp", source = "createdAt")
    OrderDetailResponse.StatusHistoryDto toStatusHistoryDto(OrderStatusHistoryEntity entity);

    // --- Named Helpers ---
    @Named("mapTotalAmount")
    default MoneyDto mapTotalAmount(OrderEntity order) {
        if (order == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(order.getTotalAmount(), order.getCurrencyCode());
    }

    @Named("calculateItemCount")
    default Integer calculateItemCount(List<OrderLineItemEntity> items) {
        if (items == null || items.isEmpty()) return 0;
        return items.stream().mapToInt(OrderLineItemEntity::getQuantity).sum();
    }

    @Named("mapPricingBreakdown")
    default OrderDetailResponse.PricingBreakdown mapPricingBreakdown(OrderEntity order) {
        if (order == null) return null;
        String currency = order.getCurrencyCode();
        return OrderDetailResponse.PricingBreakdown.builder()
                .subtotal(MoneyDto.of(order.getSubtotalAmount(), currency))
                .discount(MoneyDto.of(order.getDiscountAmount(), currency))
                .shipping(MoneyDto.of(order.getShippingAmount(), currency))
                .tax(MoneyDto.of(order.getTaxAmount(), currency))
                .total(MoneyDto.of(order.getTotalAmount(), currency))
                .build();
    }

    @Named("deserializeAddressJson")
    default AddressDto deserializeAddressJson(String addressJson) {
        if (addressJson == null || addressJson.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(addressJson, AddressDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("checkCancellable")
    default Boolean checkCancellable(String status) {
        return "PENDING_PAYMENT".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status);
    }

    @Named("mapLineUnitPrice")
    default MoneyDto mapLineUnitPrice(OrderLineItemEntity item) {
        if (item == null || item.getOrder() == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(item.getUnitPriceAmount(), item.getOrder().getCurrencyCode());
    }

    @Named("mapLineTotal")
    default MoneyDto mapLineTotal(OrderLineItemEntity item) {
        if (item == null || item.getOrder() == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(item.getLineTotalAmount(), item.getOrder().getCurrencyCode());
    }
}
