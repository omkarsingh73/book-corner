package com.bookcorner.entity.shipping;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * JPA entity representing a carrier transit milestone event.
 * Maps to shipping.tracking_milestones table.
 */
@Entity
@Table(name = "tracking_milestones", schema = "shipping")
@SQLDelete(sql = "UPDATE shipping.tracking_milestones SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TrackingMilestoneEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consignment_id", nullable = false)
    private ShippingConsignmentEntity consignment;

    @Column(name = "milestone_status", nullable = false, length = 64)
    private String milestoneStatus;

    @Column(name = "location_name", length = 150)
    private String locationName;

    @Column(name = "milestone_timestamp", nullable = false)
    private Instant milestoneTimestamp;

    @Column(name = "milestone_description", length = 255)
    private String milestoneDescription;
}
