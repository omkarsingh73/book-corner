package com.bookcorner.common.logging;

import com.bookcorner.security.SecurityUtils;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * High-performance SLF4J MDC Logging Filter.
 * Populates correlation ID, client IP, HTTP metadata, and distributed tracing IDs
 * into the MDC context for structured JSON CloudWatch log emission.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_REQUEST_URI = "requestUri";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_CLIENT_IP, resolveClientIp(request));
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());

        // Extract distributed trace and span IDs from Micrometer Tracer if present
        if (tracer != null && tracer.currentSpan() != null) {
            String traceId = tracer.currentSpan().context().traceId();
            String spanId = tracer.currentSpan().context().spanId();
            if (traceId != null) MDC.put(MDC_TRACE_ID, traceId);
            if (spanId != null) MDC.put(MDC_SPAN_ID, spanId);
        }

        // Attach correlation ID to outbound response headers
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            // Populate authenticated user if available
            SecurityUtils.getCurrentUserId().ifPresent(id -> MDC.put(MDC_USER_ID, id.toString()));

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(REQUEST_ID_HEADER);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId.trim();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
