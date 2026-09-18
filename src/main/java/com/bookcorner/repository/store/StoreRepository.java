package com.bookcorner.repository.store;

import com.bookcorner.entity.store.StoreEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for StoreEntity.
 */
@Repository
public interface StoreRepository extends JpaRepository<StoreEntity, UUID> {

    /**
     * Find store tenant by unique store code (e.g. BK_MAIN_ONLINE).
     */
    Optional<StoreEntity> findByStoreCode(String storeCode);

    /**
     * Find store by code with business policies eagerly loaded.
     */
    @EntityGraph(attributePaths = {"policy"})
    Optional<StoreEntity> findByStoreCodeAndIsActiveTrue(String storeCode);

    /**
     * Retrieve all active store tenants.
     */
    List<StoreEntity> findByIsActiveTrue();
}
