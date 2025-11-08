package com.dashboard.common.environment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "grafana")
public class GrafanaProperties {
    private String apiKey;
    private String url;
}
