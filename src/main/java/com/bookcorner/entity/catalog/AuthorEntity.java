package com.bookcorner.entity.catalog;

import com.bookcorner.common.persistence.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
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
 * JPA entity representing a book author or contributor.
 * Maps to catalog.authors table.
 */
@Entity
@Table(name = "authors", schema = "catalog")
@SQLDelete(sql = "UPDATE catalog.authors SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthorEntity extends BaseAuditEntity {

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "author_slug", nullable = false, length = 200, unique = true)
    private String authorSlug;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookAuthorEntity> bookAuthors = new ArrayList<>();
}
