package com.bookcorner.entity.ordering;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
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
 * JPA entity representing an immutable line item snapshot in an order.
 * Maps to ordering.order_line_items table.
 */
@Entity
@Table(name = "order_line_items", schema = "ordering")
@SQLDelete(sql = "UPDATE ordering.order_line_items SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderLineItemEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "format_id", nullable = false)
    private BookFormatEntity format;

    @Column(name = "book_title_snapshot", nullable = false, length = 255)
    private String bookTitleSnapshot;

    @Column(name = "isbn_13_snapshot", nullable = false, length = 13)
    private String isbn13Snapshot;

    @Column(name = "format_type_snapshot", nullable = false, length = 32)
    private String formatTypeSnapshot;

    @Column(name = "unit_price_amount", nullable = false)
    private Long unitPriceAmount;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_discount_amount", nullable = false)
    @Builder.Default
    private Long lineDiscountAmount = 0L;

    @Column(name = "line_tax_amount", nullable = false)
    @Builder.Default
    private Long lineTaxAmount = 0L;

    @Column(name = "line_total_amount", nullable = false)
    private Long lineTotalAmount;
}
