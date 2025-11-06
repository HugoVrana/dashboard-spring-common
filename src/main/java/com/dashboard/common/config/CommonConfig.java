package com.dashboard.common.config;

import com.dashboard.common.environment.GrafanaProperties;
import com.dashboard.common.logging.GrafanaHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GrafanaProperties.class)
public class CommonConfig {

    @Bean
    public GrafanaHttpClient grafanaHttpClient(GrafanaProperties grafanaProperties) {
        return new GrafanaHttpClient(grafanaProperties);
    }
}