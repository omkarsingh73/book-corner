package com.bookcorner.repository.shipping;

import com.bookcorner.entity.shipping.ShippingConsignmentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ShippingConsignmentEntity.
 */
@Repository
public interface ShippingConsignmentRepository extends JpaRepository<ShippingConsignmentEntity, UUID> {

    /**
     * Find consignment by carrier tracking number.
     */
    Optional<ShippingConsignmentEntity> findByTrackingNumber(String trackingNumber);

    /**
     * Find consignment with complete milestone timeline eagerly loaded.
     */
    @EntityGraph(attributePaths = {"milestones"})
    @Query("SELECT sc FROM ShippingConsignmentEntity sc WHERE sc.trackingNumber = :trackingNumber")
    Optional<ShippingConsignmentEntity> findByTrackingNumberWithMilestones(@Param("trackingNumber") String trackingNumber);

    /**
     * Find all consignments dispatched for an order.
     */
    List<ShippingConsignmentEntity> findByOrderId(UUID orderId);
}
