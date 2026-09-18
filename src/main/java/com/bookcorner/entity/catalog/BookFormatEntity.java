package com.bookcorner.entity.catalog;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * JPA entity representing a purchasable format SKU (PAPERBACK, HARDCOVER, EBOOK, AUDIOBOOK).
 * Maps to catalog.book_formats table.
 */
@Entity
@Table(name = "book_formats", schema = "catalog")
@SQLDelete(sql = "UPDATE catalog.book_formats SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookFormatEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private BookEntity book;

    @Column(name = "format_type", nullable = false, length = 32)
    private String formatType;

    @Column(name = "sku", nullable = false, length = 64, unique = true)
    private String sku;

    @Column(name = "base_price_amount", nullable = false)
    private Long basePriceAmount;

    @Column(name = "cost_price_amount")
    private Long costPriceAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    public int getInventoryQuantity() {
        return this.stockQuantity;
    }

    public void setInventoryQuantity(int inventoryQuantity) {
        this.stockQuantity = inventoryQuantity;
    }

    /**
     * Checks whether this book format is active (not soft-deleted).
     */
    public boolean isActive() {
        return !isDeleted();
    }

    public void setActive(boolean active) {
        setDeleted(!active);
    }
}
