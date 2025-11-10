package com.dashboard.common.logging;

import com.dashboard.common.model.log.ApiCallLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogBuilderHelper {

    private final ObjectMapper objectMapper;

    public LogBuilderHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString()).append("\n");

        StackTraceElement[] elements = ex.getStackTrace();
        int limit = Math.min(elements.length, 15);

        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(elements[i]).append("\n");
        }

        if (elements.length > limit) {
            sb.append("\t... ").append(elements.length - limit).append(" more");
        }

        return sb.toString();
    }

    public String getFullUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder(request.getRequestURL());
        String queryString = request.getQueryString();
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public String getOrCreateRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("requestId");
        if (requestId != null) {
            return (String) requestId;
        }
        return UUID.randomUUID().toString();
    }

    public String getStatusMessage(Integer statusCode) {
        return switch (statusCode / 100) {
            case 2 -> "Success";
            case 3 -> "Redirect";
            case 4 -> "Client Error";
            case 5 -> "Server Error";
            default -> "Unknown";
        };
    }

    private String extractUserId(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        return null;
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!isSensitiveHeader(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }

        return headers;
    }

    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.equals("authorization") ||
                lower.equals("cookie") ||
                lower.equals("set-cookie") ||
                lower.equals("x-api-key");
    }

    private String determineLogLevel(Integer statusCode) {
        if (statusCode >= 500) return "error";
        if (statusCode >= 400) return "warn";
        return "info";
    }

    private Map<String, Object> extractRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String characterEncoding = request.getCharacterEncoding();
                String requestBodyAsString = new String(content, characterEncoding);
                return objectMapper.readValue(requestBodyAsString, new TypeReference<>() {});
            }
        } catch (Exception e) {
        }
        return null;
    }

    private Map<String, Object> extractResponseBody(ContentCachingResponseWrapper response) {
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, response.getCharacterEncoding());
                return objectMapper.readValue(body, new TypeReference<>(){});
            }
        } catch (Exception e) {
        }
        return null;
    }

    private Long getRequestSize(ContentCachingRequestWrapper request) {
        return (long) request.getContentAsByteArray().length;
    }

    private Long getResponseSize(ContentCachingResponseWrapper response) {
        return (long) response.getContentAsByteArray().length;
    }

    public ApiCallLog.ApiCallLogBuilder buildBaseLog(String serviceName,
                                                            HttpServletRequest request,
                                                            HttpServletResponse response,
                                                            Instant timestamp,
                                                            Long durationMs) {

        Integer statusCode = response.getStatus();
        Exception exception = (Exception) request.getAttribute("exception");
        ApiCallLog.ApiCallLogBuilder builder = ApiCallLog.builder()
                .requestId(getOrCreateRequestId(request))
                .timestamp(timestamp)
                .method(request.getMethod())
                .endpoint(request.getRequestURI())
                .fullUrl(getFullUrl(request))
                .statusCode(statusCode)
                .statusMessage(getStatusMessage(statusCode))
                .level(getStatusMessage(statusCode))
                .clientIp(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .durationMs(durationMs)
                .service(serviceName)
                .userId(extractUserId(request))
                .headers(extractHeaders(request))
                .level(determineLogLevel(statusCode))
                .environment(System.getProperty("spring.profiles.active", "dev"))
                .version("1.0.0");

        if (request instanceof ContentCachingRequestWrapper) {
            builder.requestBody(extractRequestBody((ContentCachingRequestWrapper) request))
                    .requestSize(getRequestSize((ContentCachingRequestWrapper) request));
        }

        if (response instanceof ContentCachingResponseWrapper) {
            builder.responseBody(extractResponseBody((ContentCachingResponseWrapper) response))
                    .responseSize(getResponseSize((ContentCachingResponseWrapper) response));
        }

        // Only add stack trace if there's an exception
        if (exception != null) {
            builder.stackTrace(getStackTrace(exception));
        }

        return builder;
    }
}