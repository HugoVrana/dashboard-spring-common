package com.dashboard.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.http.HttpClient;

@Configuration
class HttpConfig {
    @Bean
    HttpClient httpClient() { return HttpClient.newHttpClient(); }
}
