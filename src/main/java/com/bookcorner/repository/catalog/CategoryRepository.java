package com.bookcorner.repository.catalog;

import com.bookcorner.entity.catalog.CategoryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for CategoryEntity (Hierarchical Adjacency List).
 */
@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    /**
     * Find category by unique URL-friendly slug.
     */
    Optional<CategoryEntity> findByCategorySlug(String categorySlug);

    /**
     * Retrieve all top-level root categories (parent_id IS NULL).
     */
    List<CategoryEntity> findByParentIsNullOrderByDisplayOrderAsc();

    /**
     * Retrieve complete category tree with subcategories eagerly loaded.
     */
    @EntityGraph(attributePaths = {"subcategories"})
    @Query("SELECT c FROM CategoryEntity c WHERE c.parent IS NULL ORDER BY c.displayOrder ASC")
    List<CategoryEntity> findAllRootCategoriesWithSubcategories();

    /**
     * Find immediate direct subcategories of a specific parent category.
     */
    List<CategoryEntity> findByParentIdOrderByDisplayOrderAsc(UUID parentId);
}
