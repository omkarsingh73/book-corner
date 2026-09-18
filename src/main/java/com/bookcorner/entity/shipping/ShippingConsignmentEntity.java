package com.bookcorner.entity.shipping;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.ordering.OrderEntity;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a physical carrier freight package / shipment.
 * Maps to shipping.shipping_consignments table.
 */
@Entity
@Table(name = "shipping_consignments", schema = "shipping")
@SQLDelete(sql = "UPDATE shipping.shipping_consignments SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShippingConsignmentEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "tracking_number", nullable = false, length = 128, unique = true)
    private String trackingNumber;

    @Column(name = "carrier_code", nullable = false, length = 64)
    private String carrierCode;

    @Column(name = "consignment_status", nullable = false, length = 32)
    @Builder.Default
    private String consignmentStatus = "MANIFEST_CREATED";

    @Column(name = "label_url", length = 512)
    private String labelUrl;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "actual_delivery_at")
    private Instant actualDeliveryAt;

    @Column(name = "shipping_cost_amount", nullable = false)
    private Long shippingCostAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @OneToMany(mappedBy = "consignment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("milestoneTimestamp ASC")
    @Builder.Default
    private List<TrackingMilestoneEntity> milestones = new ArrayList<>();

    public void addMilestone(String status, String location, Instant timestamp, String description) {
        TrackingMilestoneEntity milestone = TrackingMilestoneEntity.builder()
                .consignment(this)
                .milestoneStatus(status)
                .locationName(location)
                .milestoneTimestamp(timestamp)
                .milestoneDescription(description)
                .build();
        milestones.add(milestone);
        this.consignmentStatus = status;
    }
}
