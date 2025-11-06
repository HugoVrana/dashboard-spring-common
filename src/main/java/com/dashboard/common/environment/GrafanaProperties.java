package com.dashboard.common.environment;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@ConfigurationProperties(prefix = "grafana")
public class GrafanaProperties {
    private String apiKey;

    private String url;
}
