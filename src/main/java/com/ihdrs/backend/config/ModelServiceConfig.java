// ModelServiceConfig.java
package com.ihdrs.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "model-service")
public class ModelServiceConfig {

    private String baseUrl = "http://localhost:5000";
    private Duration timeout = Duration.ofSeconds(30);
    private Integer maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(1);

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 配置超时时间
        ClientHttpRequestFactory factory = clientHttpRequestFactory();
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeout.toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        return factory;
    }

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }
}