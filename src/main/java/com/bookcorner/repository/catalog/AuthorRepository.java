package com.bookcorner.repository.catalog;

import com.bookcorner.entity.catalog.AuthorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for AuthorEntity.
 */
@Repository
public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID>, JpaSpecificationExecutor<AuthorEntity> {

    /**
     * Find author by unique URL-friendly slug.
     */
    Optional<AuthorEntity> findByAuthorSlug(String authorSlug);

    /**
     * Check if author slug is already taken.
     */
    boolean existsByAuthorSlug(String authorSlug);

    /**
     * Search authors by name with case-insensitive contains match.
     */
    Page<AuthorEntity> findByFullNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Retrieve author with their complete bibliography.
     */
    @Query("""
        SELECT a FROM AuthorEntity a 
        LEFT JOIN FETCH a.bookAuthors ba 
        LEFT JOIN FETCH ba.book b 
        WHERE a.id = :authorId
        """)
    Optional<AuthorEntity> findByIdWithBibliography(@Param("authorId") UUID authorId);
}
