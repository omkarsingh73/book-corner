package com.bookcorner.entity.ordering;

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
 * JPA entity representing an individual item included in an RMA return request.
 * Maps to ordering.return_request_items table.
 */
@Entity
@Table(name = "return_request_items", schema = "ordering")
@SQLDelete(sql = "UPDATE ordering.return_request_items SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ReturnRequestItemEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequestEntity returnRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_line_item_id", nullable = false)
    private OrderLineItemEntity orderLineItem;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "item_condition", nullable = false, length = 32)
    @Builder.Default
    private String itemCondition = "UNOPENED";
}
