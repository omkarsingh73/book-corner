package com.bookcorner.repository.catalog;

import com.bookcorner.entity.catalog.PublisherEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PublisherEntity.
 */
@Repository
public interface PublisherRepository extends JpaRepository<PublisherEntity, UUID> {

    /**
     * Find publisher by its business code.
     */
    Optional<PublisherEntity> findByPublisherCode(String publisherCode);

    /**
     * Search publishers by name.
     */
    Page<PublisherEntity> findByPublisherNameContainingIgnoreCase(String name, Pageable pageable);
}
