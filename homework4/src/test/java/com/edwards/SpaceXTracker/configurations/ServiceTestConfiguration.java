package com.edwards.SpaceXTracker.configurations;

import com.edwards.SpaceXTracker.api.ApiClient;
import com.edwards.SpaceXTracker.config.ApiConfiguration;
import com.edwards.SpaceXTracker.services.SpaceXService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:configuration.properties")
public class ServiceTestConfiguration {
    @Bean
    public ApiConfiguration apiConfiguration() {
        return new ApiConfiguration();
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
