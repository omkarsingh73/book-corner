package com.bookcorner.repository.catalog;

import com.bookcorner.entity.catalog.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for BookEntity.
 * Supports JpaSpecificationExecutor for multi-faceted filtering,
 * EntityGraphs for N+1 prevention, and PostgreSQL full-text search.
 */
@Repository
public interface BookRepository extends JpaRepository<BookEntity, UUID>, JpaSpecificationExecutor<BookEntity> {

    /**
     * Override default findAll with EntityGraph to eagerly fetch associations
     * and eliminate N+1 queries during catalog browsing and mapping.
     */
    @Override
    @EntityGraph(attributePaths = {"publisher", "primaryCategory", "formats", "bookAuthors.author"})
    Page<BookEntity> findAll(Specification<BookEntity> spec, Pageable pageable);

    /**
     * Find book by unique ISBN-13 with eagerly fetched publisher and primary category.
     */
    @EntityGraph(attributePaths = {"publisher", "primaryCategory"})
    Optional<BookEntity> findByIsbn13(String isbn13);

    /**
     * Find book by ID with eager fetch of publisher, primary category, formats, and authors.
     */
    @EntityGraph(attributePaths = {"publisher", "primaryCategory", "formats", "bookAuthors.author"})
    @Query("SELECT b FROM BookEntity b WHERE b.id = :id")
    Optional<BookEntity> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Check if an active book exists with the given ISBN-13.
     */
    boolean existsByIsbn13(String isbn13);

    /**
     * Find books belonging to a specific primary category with pagination.
     */
    @EntityGraph(attributePaths = {"publisher", "primaryCategory"})
    Page<BookEntity> findByPrimaryCategoryId(UUID categoryId, Pageable pageable);

    /**
     * Find books published by a specific publishing house.
     */
    Page<BookEntity> findByPublisherId(UUID publisherId, Pageable pageable);

    /**
     * Find books written by a specific author via book_authors join.
     */
    @Query("SELECT b FROM BookEntity b JOIN b.bookAuthors ba WHERE ba.author.id = :authorId")
    Page<BookEntity> findByAuthorId(@Param("authorId") UUID authorId, Pageable pageable);

    /**
     * Native PostgreSQL Full-Text Search utilizing GIN index over title, subtitle, and synopsis.
     * Orders results by text relevance rank.
     */
    @Query(value = """
        SELECT b.* FROM catalog.books b
        WHERE b.is_deleted = false
          AND to_tsvector('english', b.title || ' ' || COALESCE(b.subtitle, '') || ' ' || COALESCE(b.synopsis, '')) 
              @@ websearch_to_tsquery('english', :keyword)
        ORDER BY ts_rank(
            to_tsvector('english', b.title || ' ' || COALESCE(b.subtitle, '') || ' ' || COALESCE(b.synopsis, '')),
            websearch_to_tsquery('english', :keyword)
        ) DESC
        """,
        countQuery = """
        SELECT count(*) FROM catalog.books b
        WHERE b.is_deleted = false
          AND to_tsvector('english', b.title || ' ' || COALESCE(b.subtitle, '') || ' ' || COALESCE(b.synopsis, '')) 
              @@ websearch_to_tsquery('english', :keyword)
        """,
        nativeQuery = true)
    Page<BookEntity> searchByFullText(@Param("keyword") String keyword, Pageable pageable);
}
