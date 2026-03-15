package com.edwards.SpaceXTracker.api;

import com.edwards.SpaceXTracker.config.ApiConfiguration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

@Component
@AllArgsConstructor
public class ApiClient {
    private final ApiConfiguration apiConfiguration;

    /**
     * The very direct fetch along with auto JSON parse
     * @param targetUrl request
     * @param clazz json DTO
     * @return mapped response
     * @param <T> DTO
     * @throws IOException all response reading errors
     * @throws IllegalArgumentException when request is ill formed
     */
    public <T> ApiResponse<T> get(String targetUrl, Class<T> clazz) throws IOException, IllegalArgumentException {
        String target = apiConfiguration.getBaseUrl() + targetUrl;
        int timeout = apiConfiguration.getTimeout();
        HttpURLConnection connection = null;
        ApiResponse<T> response;

        try {

            URL url = URI.create(target).toURL();
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("Accept", "application/json");

            response = ApiResponse.fromConnection(connection, clazz);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }
}