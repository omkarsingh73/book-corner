package com.bookcorner.entity.ordering;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a customer Return Merchandise Authorization (RMA).
 * Maps to ordering.return_requests table.
 */
@Entity
@Table(name = "return_requests", schema = "ordering")
@SQLDelete(sql = "UPDATE ordering.return_requests SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReturnRequestEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "rma_number", nullable = false, length = 64, unique = true)
    private String rmaNumber;

    @Column(name = "return_status", nullable = false, length = 32)
    @Builder.Default
    private String returnStatus = "REQUESTED";

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "reason_notes", columnDefinition = "TEXT")
    private String reasonNotes;

    @Column(name = "refund_amount", nullable = false)
    @Builder.Default
    private Long refundAmount = 0L;

    @Column(name = "tracking_number", length = 128)
    private String trackingNumber;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReturnRequestItemEntity> returnItems = new ArrayList<>();

    public void addReturnItem(ReturnRequestItemEntity item) {
        returnItems.add(item);
        item.setReturnRequest(this);
    }
}
