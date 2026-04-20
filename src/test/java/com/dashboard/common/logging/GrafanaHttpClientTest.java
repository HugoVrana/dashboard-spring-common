package com.dashboard.common.logging;

import com.dashboard.common.environment.GrafanaProperties;
import com.dashboard.common.model.log.ApiCallLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrafanaHttpClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private GrafanaHttpClient grafanaHttpClient;

    private static final String GRAFANA_URL = "https://logs-prod-039.grafana.net/loki/api/v1/push";
    private static final String API_KEY = "1358926:ENTER_YOUR_API_KEY";

    @BeforeEach
    void setUp() {
        GrafanaProperties properties = new GrafanaProperties();
        properties.setUrl(GRAFANA_URL);
        properties.setApiKey(API_KEY);
        grafanaHttpClient = new GrafanaHttpClient(properties, httpClient);
    }

    private ApiCallLog buildLog() {
        return ApiCallLog.builder()
                .requestId("req-123")
                .method("GET")
                .endpoint("/api/test")
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .statusCode(200)
                .service("test-service")
                .environment("test")
                .level("INFO")
                .build();
    }

    @SuppressWarnings("unchecked")
    private void stubHttpClientSuccess() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{}");
    }

    // ==================== Happy Path ====================

    @Test
    void send_validLog_makesExactlyOneHttpRequest() throws Exception {
        stubHttpClientSuccess();

        grafanaHttpClient.send(buildLog());

        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_validLog_usesConfiguredUrl() throws Exception {
        stubHttpClientSuccess();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        grafanaHttpClient.send(buildLog());

        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals(GRAFANA_URL, captor.getValue().uri().toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_validLog_usesPostMethod() throws Exception {
        stubHttpClientSuccess();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        grafanaHttpClient.send(buildLog());

        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("POST", captor.getValue().method());
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_validLog_includesBearerAuthorizationHeader() throws Exception {
        stubHttpClientSuccess();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        grafanaHttpClient.send(buildLog());

        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        String authHeader = captor.getValue().headers().firstValue("Authorization").orElse("");
        assertEquals("Bearer " + API_KEY, authHeader);
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_validLog_contentTypeIsJson() throws Exception {
        stubHttpClientSuccess();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        grafanaHttpClient.send(buildLog());

        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        String contentType = captor.getValue().headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json", contentType);
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_validLog_requestBodyIsPresent() throws Exception {
        stubHttpClientSuccess();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        grafanaHttpClient.send(buildLog());

        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertTrue(captor.getValue().bodyPublisher().isPresent());
        assertTrue(captor.getValue().bodyPublisher().get().contentLength() > 0);
    }

    // ==================== Error Handling ====================

    @Test
    void send_httpClientThrowsRuntimeException_doesNotPropagate() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertDoesNotThrow(() -> grafanaHttpClient.send(buildLog()));
    }

    @Test
    void send_httpClientThrowsInterruptedException_doesNotPropagate() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("Interrupted"));

        assertDoesNotThrow(() -> grafanaHttpClient.send(buildLog()));
    }

    @Test
    void send_httpClientThrowsIOException_doesNotPropagate() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new java.io.IOException("I/O error"));

        assertDoesNotThrow(() -> grafanaHttpClient.send(buildLog()));
    }

    // ==================== Log Field Mapping ====================

    @Test
    @SuppressWarnings("unchecked")
    void send_differentApiKey_authHeaderReflectsNewKey() throws Exception {
        GrafanaProperties properties = new GrafanaProperties();
        properties.setUrl(GRAFANA_URL);
        properties.setApiKey("different-key");
        GrafanaHttpClient client = new GrafanaHttpClient(properties, httpClient);

        stubHttpClientSuccess();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        client.send(buildLog());

        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("Bearer different-key", captor.getValue().headers().firstValue("Authorization").orElse(""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_differentUrl_requestSentToNewUrl() throws Exception {
        String alternateUrl = "http://other-grafana.example.com/loki/api/v1/push";
        GrafanaProperties properties = new GrafanaProperties();
        properties.setUrl(alternateUrl);
        properties.setApiKey(API_KEY);
        GrafanaHttpClient client = new GrafanaHttpClient(properties, httpClient);

        stubHttpClientSuccess();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);

        client.send(buildLog());

        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals(alternateUrl, captor.getValue().uri().toString());
    }
}
