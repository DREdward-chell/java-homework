package com.edwards.SpaceXTracker.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:configuration.properties")
@ConfigurationProperties(prefix = "api")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ApiConfiguration {
    private String baseUrl = "https://api.spacexdata.com/";
    private int timeout = 5000;
}