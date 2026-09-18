package com.bookcorner.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hierarchical category tree node representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryNodeDto implements Serializable {

    private UUID categoryId;
    private String name;
    private String slug;
    private Integer level;
    private Integer displayOrder;

    @Builder.Default
    private List<CategoryNodeDto> subcategories = new ArrayList<>();
}
