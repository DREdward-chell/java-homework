package com.edwards.SpaceXTracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "api")
@Data
public class ApiConfiguration {
    private String baseUrl = "https://api.spacexdata.com/";
    private int timeout = 5000;
}