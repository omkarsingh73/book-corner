package com.bookcorner.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Generic acknowledgement message response matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericMessageResponse implements Serializable {

    private String message;

    public static GenericMessageResponse of(String message) {
        return new GenericMessageResponse(message);
    }
}
