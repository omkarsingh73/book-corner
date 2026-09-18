package com.bookcorner.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Standard RFC-7807 compatible error envelope matching OpenAPI ErrorEnvelope schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorEnvelope implements Serializable {

    @Builder.Default
    private boolean success = false;

    private ErrorDetail error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail implements Serializable {
        private String code;
        private String message;

        @Builder.Default
        private List<FieldErrorDetail> details = new ArrayList<>();

        @Builder.Default
        private Instant timestamp = Instant.now();

        @Builder.Default
        private String requestId = UUID.randomUUID().toString();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldErrorDetail implements Serializable {
        private String field;
        private String issue;
    }

    public static ErrorEnvelope of(String code, String message) {
        return ErrorEnvelope.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .timestamp(Instant.now())
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .build();
    }

    public static ErrorEnvelope of(String code, String message, List<FieldErrorDetail> details) {
        return ErrorEnvelope.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .timestamp(Instant.now())
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .build();
    }
}
