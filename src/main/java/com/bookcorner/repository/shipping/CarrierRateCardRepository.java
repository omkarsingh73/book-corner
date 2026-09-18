package com.bookcorner.repository.shipping;

import com.bookcorner.entity.shipping.CarrierRateCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for CarrierRateCardEntity.
 */
@Repository
public interface CarrierRateCardRepository extends JpaRepository<CarrierRateCardEntity, UUID> {

    /**
     * Find all active rate cards for an origin-destination country route.
     */
    @Query("""
        SELECT rc FROM CarrierRateCardEntity rc 
        WHERE rc.originCountryCode = :origin 
          AND rc.destinationCountryCode = :destination 
          AND rc.isActive = true
        ORDER BY rc.baseRateAmount ASC
        """)
    List<CarrierRateCardEntity> findActiveRatesByRoute(
            @Param("origin") String origin,
            @Param("destination") String destination
    );

    /**
     * Find active rate cards for a specific carrier.
     */
    List<CarrierRateCardEntity> findByCarrierCodeAndIsActiveTrue(String carrierCode);
}
