package com.bookcorner.entity.ordering;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.payment.PaymentTransactionEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing a confirmed customer order aggregate.
 * Maps to ordering.orders table.
 */
@Entity
@Table(name = "orders", schema = "ordering")
@SQLDelete(sql = "UPDATE ordering.orders SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderEntity extends BaseAuditEntity {

    @Column(name = "order_number", nullable = false, length = 64, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private CouponEntity coupon;

    @Column(name = "order_status", nullable = false, length = 32)
    @Builder.Default
    private String orderStatus = "DRAFT";

    @Column(name = "subtotal_amount", nullable = false)
    private Long subtotalAmount;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Long discountAmount = 0L;

    @Column(name = "shipping_amount", nullable = false)
    @Builder.Default
    private Long shippingAmount = 0L;

    @Column(name = "tax_amount", nullable = false)
    @Builder.Default
    private Long taxAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address_snapshot", nullable = false, columnDefinition = "jsonb")
    private String shippingAddressSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address_snapshot", nullable = false, columnDefinition = "jsonb")
    private String billingAddressSnapshot;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<OrderLineItemEntity> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<OrderStatusHistoryEntity> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<PaymentTransactionEntity> paymentTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReturnRequestEntity> returnRequests = new ArrayList<>();

    public void addLineItem(OrderLineItemEntity item) {
        lineItems.add(item);
        item.setOrder(this);
    }

    public void addStatusTransition(String fromStatus, String toStatus, String notes) {
        OrderStatusHistoryEntity history = OrderStatusHistoryEntity.builder()
                .order(this)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .transitionNotes(notes)
                .build();
        statusHistory.add(history);
        this.orderStatus = toStatus;
    }
}
