package com.edwards.SpaceXTracker.services;

import com.edwards.SpaceXTracker.api.ApiClient;
import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.exceptions.SpaceXIOException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@AllArgsConstructor
public class SpaceXService {
    ApiClient apiClient;

    public ApiResponse<Launch> tryGet(String url) throws SpaceXIOException {
        try {
            return apiClient.get(url);
        } catch (IOException e) {
            throw new SpaceXIOException("An error occured on fetching " + url);
        }
    }

    public ApiResponse<Launch> getLaunchByID(String id) throws SpaceXIOException {
        return tryGet("v5/launches/" + id);
    }
}