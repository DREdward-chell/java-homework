package com.edwards.SpaceXTracker.configurations;

import com.edwards.SpaceXTracker.api.ApiClient;
import com.edwards.SpaceXTracker.config.ApiConfiguration;
import com.edwards.SpaceXTracker.services.SpaceXService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:configuration.properties")
public class ServiceTestConfiguration {
    @Value("${api.base-url}")
    private String baseUrl;

    @Value("${api.timeout}")
    private int timeout;

    @Bean
    public ApiConfiguration apiConfiguration() {
        return new ApiConfiguration(baseUrl, timeout);
    }

    @Bean
    public ApiClient apiClient() {
        return new ApiClient(apiConfiguration());
    }

    @Bean
    public SpaceXService spaceXService() {
        return new SpaceXService(apiClient());
    }
}
