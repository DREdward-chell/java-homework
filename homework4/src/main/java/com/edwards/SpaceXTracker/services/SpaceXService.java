package com.edwards.SpaceXTracker.services;

import com.edwards.SpaceXTracker.api.ApiClient;
import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.exceptions.BaseSpaceXAppException;
import com.edwards.SpaceXTracker.exceptions.SpaceXIOException;
import com.edwards.SpaceXTracker.exceptions.SpaceXMalformedUrl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * This service is responsible for all fetching.
 * All exceptions are converted to children of {@link BaseSpaceXAppException}
 * to allow automatic exit with proper error message
 */
@Component
@AllArgsConstructor
public class SpaceXService {
    ApiClient apiClient;

    /**
     * A simple wrapper that automatically converts {@link Exception} instances to {@link BaseSpaceXAppException}
     * @param url endpoint
     * @return mapped response
     * @throws SpaceXIOException parsing/reading issues
     * @throws SpaceXMalformedUrl corrupt url
     */
    public ApiResponse<Launch> tryGet(String url) throws SpaceXIOException, SpaceXMalformedUrl {
        try {
            return apiClient.get(url, Launch.class);
        } catch (IOException e) {
            throw new SpaceXIOException("An error occured on fetching " + url);
        } catch (IllegalArgumentException e) {
            throw new SpaceXMalformedUrl("Url seems to be malformed: " + url);
        }
    }

    public ApiResponse<Launch> getLaunchByID(String id) throws SpaceXIOException, SpaceXMalformedUrl {
        return tryGet("v5/launches/" + id);
    }
}