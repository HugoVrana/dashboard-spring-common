package com.dashboard.common.environment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@ConfigurationProperties(prefix = "grafana")
public class GrafanaProperties {
    @Value("${GRAFANA_API_KEY}")
    private String apiKey;

    @Value("${GRAFANA_LOG_PUSH_API_URL}")
    private String url;
}
