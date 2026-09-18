package com.bookcorner.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Typeahead search suggestion item matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionDto implements Serializable {

    private String type; // BOOK, AUTHOR, CATEGORY
    private String id;
    private String text;
}
