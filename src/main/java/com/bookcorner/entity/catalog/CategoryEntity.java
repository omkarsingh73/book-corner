package com.bookcorner.entity.catalog;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a hierarchical catalog category node.
 * Maps to catalog.categories table (Adjacency List Tree).
 */
@Entity
@Table(name = "categories", schema = "catalog")
@SQLDelete(sql = "UPDATE catalog.categories SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CategoryEntity extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private CategoryEntity parent;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "category_slug", nullable = false, length = 100, unique = true)
    private String categorySlug;

    @Column(name = "tree_level", nullable = false)
    @Builder.Default
    private int categoryLevel = 1;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<CategoryEntity> subcategories = new ArrayList<>();

    @OneToMany(mappedBy = "primaryCategory", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookEntity> books = new ArrayList<>();

    public void addSubcategory(CategoryEntity child) {
        subcategories.add(child);
        child.setParent(this);
        child.setCategoryLevel(this.categoryLevel + 1);
    }
}
