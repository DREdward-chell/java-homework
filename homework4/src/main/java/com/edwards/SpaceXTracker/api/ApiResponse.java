package com.edwards.SpaceXTracker.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.time.Instant;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ApiResponse<DTO> {
    private int status;
    private Instant timestamp;
    private String message;
    private String error;
    private DTO data;

    public static <T> ApiResponse<T> fromConnection(HttpURLConnection connection) throws IOException {
        ApiResponse<T> response = new ApiResponse<>();

        response.status = connection.getResponseCode();
        response.timestamp = Instant.now();

        if (ApiMapping.isError(response.status)) {
            response.error = connection.getResponseMessage();
        } else {
            response.message = connection.getResponseMessage();
            InputStream is = connection.getInputStream();
            Reader reader = new InputStreamReader(is);

            TypeReference<T> typeRef = new TypeReference<>() {};

            ObjectMapper mapper = new ObjectMapper();
            response.data = mapper.readValue(reader, typeRef);
        }

        return response;
    }
}
