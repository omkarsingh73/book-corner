package com.bookcorner.mapper;

import com.bookcorner.dto.cart.CartLineItemDto;
import com.bookcorner.dto.cart.CartResponse;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.entity.catalog.BookAuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.ordering.CartEntity;
import com.bookcorner.entity.ordering.CartItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for Cart aggregates and items.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface CartMapper {

    @Mapping(target = "cartId", source = "id")
    @Mapping(target = "couponCode", source = "appliedCoupon.couponCode")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalQuantity", source = "items", qualifiedByName = "calculateTotalQuantity")
    @Mapping(target = "subtotal", source = "items", qualifiedByName = "calculateSubtotal")
    @Mapping(target = "discount", source = "entity", qualifiedByName = "defaultZeroMoney")
    @Mapping(target = "estimatedShipping", source = "entity", qualifiedByName = "defaultZeroMoney")
    @Mapping(target = "estimatedTax", source = "entity", qualifiedByName = "defaultZeroMoney")
    @Mapping(target = "total", source = "items", qualifiedByName = "calculateSubtotal")
    CartResponse toCartResponse(CartEntity entity);

    @Mapping(target = "itemId", source = "id")
    @Mapping(target = "formatId", source = "format.id")
    @Mapping(target = "bookId", source = "format.book.id")
    @Mapping(target = "bookTitle", source = "format.book.title")
    @Mapping(target = "authorNames", source = "format.book", qualifiedByName = "formatAuthorNames")
    @Mapping(target = "coverImageUrl", source = "format.book.coverImageUrl")
    @Mapping(target = "formatType", source = "format.formatType")
    @Mapping(target = "sku", source = "format.sku")
    @Mapping(target = "unitPrice", source = "format", qualifiedByName = "mapFormatUnitPrice")
    @Mapping(target = "lineTotal", source = "entity", qualifiedByName = "calculateLineTotal")
    CartLineItemDto toCartLineItemDto(CartItemEntity entity);

    List<CartLineItemDto> toCartLineItemDtoList(List<CartItemEntity> entities);

    @Named("formatAuthorNames")
    default String formatAuthorNames(BookEntity book) {
        if (book == null || book.getBookAuthors() == null || book.getBookAuthors().isEmpty()) {
            return "Unknown Author";
        }
        return book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getFullName())
                .collect(Collectors.joining(", "));
    }

    @Named("mapFormatUnitPrice")
    default MoneyDto mapFormatUnitPrice(BookFormatEntity format) {
        if (format == null) return MoneyDto.of(0, "USD");
        return MoneyDto.of(format.getBasePriceAmount(), format.getCurrencyCode());
    }

    @Named("calculateLineTotal")
    default MoneyDto calculateLineTotal(CartItemEntity item) {
        if (item == null || item.getFormat() == null) return MoneyDto.of(0, "USD");
        long lineTotalCents = item.getFormat().getBasePriceAmount() * item.getQuantity();
        return MoneyDto.of(lineTotalCents, item.getFormat().getCurrencyCode());
    }

    @Named("calculateTotalQuantity")
    default Integer calculateTotalQuantity(List<CartItemEntity> items) {
        if (items == null || items.isEmpty()) return 0;
        return items.stream().mapToInt(CartItemEntity::getQuantity).sum();
    }

    @Named("calculateSubtotal")
    default MoneyDto calculateSubtotal(List<CartItemEntity> items) {
        if (items == null || items.isEmpty()) return MoneyDto.of(0, "USD");
        long totalCents = items.stream()
                .mapToLong(i -> i.getFormat().getBasePriceAmount() * i.getQuantity())
                .sum();
        String currency = items.get(0).getFormat().getCurrencyCode();
        return MoneyDto.of(totalCents, currency);
    }

    @Named("defaultZeroMoney")
    default MoneyDto defaultZeroMoney(CartEntity entity) {
        return MoneyDto.of(0, "USD");
    }
}
