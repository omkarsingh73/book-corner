package com.bookcorner.dto.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.io.Serializable;

/**
 * Standard offset pagination metadata envelope.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMetaDto implements Serializable {

    @NotNull
    @Min(1)
    private Integer page;

    @NotNull
    @Min(1)
    private Integer size;

    @NotNull
    @Min(0)
    private Long totalElements;

    @NotNull
    @Min(0)
    private Integer totalPages;

    @NotNull
    private Boolean hasNext;

    @NotNull
    private Boolean hasPrevious;

    public static PageMetaDto fromPage(Page<?> page) {
        return PageMetaDto.builder()
                .page(page.getNumber() + 1) // 1-indexed for client readability
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
