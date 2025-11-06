package com.dashboard.common.environment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "grafana")
public class GrafanaProperties {
    private String apiKey;

    private String url;
}
