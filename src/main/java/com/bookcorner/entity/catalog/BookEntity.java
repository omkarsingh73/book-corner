package com.bookcorner.entity.catalog;

import com.bookcorner.common.persistence.BaseAuditEntity;
import com.bookcorner.entity.review.ReviewEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a canonical book master record.
 * Maps to catalog.books table.
 */
@Entity
@Table(name = "books", schema = "catalog")
@SQLDelete(sql = "UPDATE catalog.books SET is_deleted = true, deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BookEntity extends BaseAuditEntity {

    @Column(name = "isbn_13", nullable = false, length = 13, unique = true)
    private String isbn13;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id", nullable = false)
    private PublisherEntity publisher;

    @Column(name = "publication_date", nullable = false)
    private LocalDate publicationDate;

    @Column(name = "language", nullable = false, length = 32)
    @Builder.Default
    private String primaryLanguage = "English";

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_category_id", nullable = false)
    private CategoryEntity primaryCategory;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<BookAuthorEntity> bookAuthors = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    @Builder.Default
    private List<BookFormatEntity> formats = new ArrayList<>();

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReviewEntity> reviews = new ArrayList<>();

    public void addFormat(BookFormatEntity format) {
        formats.add(format);
        format.setBook(this);
    }

    public void removeFormat(BookFormatEntity format) {
        formats.remove(format);
        format.setBook(null);
    }

    public void addAuthor(AuthorEntity author, int order, String role) {
        BookAuthorEntity bookAuthor = BookAuthorEntity.builder()
                .book(this)
                .author(author)
                .authorOrder(order)
                .contributionRole(role)
                .build();
        bookAuthors.add(bookAuthor);
    }
}
