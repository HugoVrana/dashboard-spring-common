package com.dashboard.common.model.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Description;

@Data
@Builder
public class ApiCallLog {
    // Request Information
    // Unique identifier for tracing
    @NonNull
    private String requestId;

    // GET, POST, PUT, DELETE, etc.
    @NonNull
    private String method;

    // /api/users/{id}
    @NonNull
    private String endpoint;

    // Full request URL with query params
    private String fullUrl;

    // Timing Information
    // When the request started
    @NonNull private Instant timestamp;

    // How long the request took
    private Long durationMs;

    // Response Information
    // HTTP status code (200, 404, 500, etc.)
    @NonNull
    private Integer statusCode;

    // OK, Not Found, Internal Server Error
    private String statusMessage;

    // Client Information
    // Client IP address
    private String clientIp;

    // Browser/client info
    private String userAgent;

    // If authenticated, user identifier
    private String userId;

    // Error Information (optional)
    // Error message if the request failed
    private String errorMessage;

    // Exception class name
    private String errorType;

    // Stack trace for errors (be careful with PII)
    private String stackTrace;

    // Additional Context
    // Important headers (filtered)
    private Map<String, String> headers;

    // Request payload (sanitized)
    private Map<String, Object> requestBody;

    // Response payload (sanitized)
    private Map<String, Object> responseBody;

    // Any custom metadata
    private Map<String, String> customFields;

    // Performance Metrics
    // Request body size in bytes
    private Long requestSize;

    // Response body size in bytes
    private Long responseSize;

    // Business Context
    // Service name
    @NonNull private String service;

    // dev, staging, prod
    private String environment;

    // API version
    private String version;

    // Level
    private String level;


    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "ApiCallLog{error serializing to JSON: " + e.getMessage() + "}";
        }
    }
}
