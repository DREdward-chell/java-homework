package com.edwards.SpaceXTracker.api;

import com.edwards.SpaceXTracker.config.ApiConfiguration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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

    /**
     * POST request with a JSON body
     * @param targetUrl endpoint path
     * @param jsonBody serialized JSON request body
     * @param clazz DTO class for response parsing
     * @return mapped response
     * @param <T> DTO
     * @throws IOException all response reading/writing errors
     * @throws IllegalArgumentException when request URL is ill formed
     */
    public <T> ApiResponse<T> post(String targetUrl, String jsonBody, Class<T> clazz) throws IOException, IllegalArgumentException {
        String target = apiConfiguration.getBaseUrl() + targetUrl;
        int timeout = apiConfiguration.getTimeout();
        HttpURLConnection connection = null;
        ApiResponse<T> response;

        try {

            URL url = URI.create(target).toURL();
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            response = ApiResponse.fromConnection(connection, clazz);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }
}
