package com.edwards.SpaceXTracker.api;

import com.edwards.SpaceXTracker.config.ApiConfiguration;
import com.edwards.SpaceXTracker.exceptions.SpaceXIOException;
import com.edwards.SpaceXTracker.exceptions.SpaceXMalformedUrl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

@Component
@AllArgsConstructor
public class ApiClient {
    private final ApiConfiguration apiConfiguration;

    public <T> ApiResponse<T> get(String targetUrl) throws IOException, IllegalArgumentException {
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

            response = ApiResponse.fromConnection(connection);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }
}