package com.dashboard.common.logging;

import com.dashboard.common.environment.GrafanaProperties;
import com.dashboard.common.model.log.ApiCallLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class GrafanaHttpClient {

    private final GrafanaProperties properties;
    private final HttpClient httpClient;

    public void send(ApiCallLog apiCallLog) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Instant instant = Instant.now();
            String timestampNanos = String.valueOf(instant.getEpochSecond() * 1_000_000_000L + instant.getNano());

            Map<String, Object> payload = Map.of(
                    "streams", List.of(
                            Map.of(
                                    "stream", Map.of(
                                            "service", apiCallLog.getService(),
                                            "environment", apiCallLog.getEnvironment(),
                                            "level", apiCallLog.getLevel()
                                    ),
                                    "values", List.of(
                                            List.of(timestampNanos, apiCallLog.toString())
                                    )
                            )
                    )
            );
            String body = mapper.writeValueAsString(payload);
            System.out.println("Sending to Grafana: " + body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}