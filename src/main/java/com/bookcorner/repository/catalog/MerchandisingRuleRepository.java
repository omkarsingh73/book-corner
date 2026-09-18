package com.bookcorner.repository.catalog;

import com.bookcorner.entity.catalog.MerchandisingRuleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for MerchandisingRuleEntity (Up-Sell / Cross-Sell recommendations).
 */
@Repository
public interface MerchandisingRuleRepository extends JpaRepository<MerchandisingRuleEntity, UUID> {

    /**
     * Find active UP-SELL rules for a source book ordered by priority.
     */
    @EntityGraph(attributePaths = {"targetBook.publisher", "targetBook.primaryCategory"})
    @Query("""
        SELECT mr FROM MerchandisingRuleEntity mr 
        WHERE mr.sourceBook.id = :bookId 
          AND mr.ruleType = 'UP_SELL' 
          AND mr.isActive = true 
        ORDER BY mr.priority ASC
        """)
    List<MerchandisingRuleEntity> findActiveUpSellsBySourceBookId(@Param("bookId") UUID bookId);

    /**
     * Find active CROSS-SELL companion titles for a source book ordered by priority.
     */
    @EntityGraph(attributePaths = {"targetBook.publisher", "targetBook.primaryCategory"})
    @Query("""
        SELECT mr FROM MerchandisingRuleEntity mr 
        WHERE mr.sourceBook.id = :bookId 
          AND mr.ruleType = 'CROSS_SELL' 
          AND mr.isActive = true 
        ORDER BY mr.priority ASC
        """)
    List<MerchandisingRuleEntity> findActiveCrossSellsBySourceBookId(@Param("bookId") UUID bookId);
}
