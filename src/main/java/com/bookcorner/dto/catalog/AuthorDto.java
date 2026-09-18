package com.bookcorner.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Author profile summary representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDto implements Serializable {

    private UUID authorId;
    private String fullName;
    private String authorSlug;
    private String biography;
    private String avatarUrl;
}
