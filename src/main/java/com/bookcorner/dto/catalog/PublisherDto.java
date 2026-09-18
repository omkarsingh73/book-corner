package com.bookcorner.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Publisher profile representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublisherDto implements Serializable {

    private UUID publisherId;
    private String publisherName;
    private String publisherCode;
    private String websiteUrl;
}
