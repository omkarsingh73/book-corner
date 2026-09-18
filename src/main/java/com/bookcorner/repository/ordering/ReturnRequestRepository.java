package com.bookcorner.repository.ordering;

import com.bookcorner.entity.ordering.ReturnRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ReturnRequestEntity (RMA).
 */
@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequestEntity, UUID> {

    /**
     * Find return authorization by unique RMA number.
     */
    Optional<ReturnRequestEntity> findByRmaNumber(String rmaNumber);

    /**
     * Find return request with return items eagerly loaded.
     */
    @EntityGraph(attributePaths = {"returnItems.orderLineItem"})
    @Query("SELECT r FROM ReturnRequestEntity r WHERE r.rmaNumber = :rmaNumber")
    Optional<ReturnRequestEntity> findByRmaNumberWithItems(@Param("rmaNumber") String rmaNumber);

    /**
     * Find all return requests associated with an order.
     */
    Page<ReturnRequestEntity> findByOrderId(UUID orderId, Pageable pageable);
}
