package com.edwards.SpaceXTracker.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.google.gson.Gson;
import java.io.*;
import java.net.HttpURLConnection;
import java.time.Instant;

/**
 * The response class is a very simple utility to map responses from URL connection
 * @param <DTO> dataclass used for JSON mapping using {@link Gson}
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ApiResponse<DTO> {
    private int status;
    private Instant timestamp;
    private String message;
    private String error;
    private DTO data;

    /**
     * A very simple factory that allows to map the response directly from connection
     * @param connection built request
     * @param clazz dto class
     * @return mapped response
     * @param <T> dto
     * @throws IOException I decided to leave the error handling to service
     */
    public static <T> ApiResponse<T> fromConnection(HttpURLConnection connection, Class<T> clazz) throws IOException {
        ApiResponse<T> response = new ApiResponse<>();

        response.status = connection.getResponseCode();
        response.timestamp = Instant.now();

        if (ApiMapping.isError(response.status)) {
            response.error = connection.getResponseMessage();
        } else {
            response.message = connection.getResponseMessage();
            InputStream is = connection.getInputStream();
            Reader reader = new BufferedReader(new InputStreamReader(is));

            Gson mapper = new Gson();

            response.data = mapper.fromJson(reader, clazz);
        }

        return response;
    }
}
