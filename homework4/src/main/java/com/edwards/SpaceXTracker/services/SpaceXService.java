package com.edwards.SpaceXTracker.services;

import com.edwards.SpaceXTracker.api.ApiClient;
import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.JsonBuilder;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.api.DTO.QueryResponse;
import com.edwards.SpaceXTracker.cache.FileStorage;
import com.edwards.SpaceXTracker.exceptions.BaseSpaceXAppException;
import com.edwards.SpaceXTracker.exceptions.SpaceXIOException;
import com.edwards.SpaceXTracker.exceptions.SpaceXMalformedUrl;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * This service is responsible for all fetching.
 * All exceptions are converted to children of {@link BaseSpaceXAppException}
 * to allow automatic exit with proper error message
 */
@Component
@AllArgsConstructor
public class SpaceXService {
    ApiClient apiClient;
    FileStorage fileStorage;

    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * A simple wrapper that automatically converts {@link Exception} instances to {@link BaseSpaceXAppException}
     * @param url endpoint
     * @param clazz DTO class
     * @return mapped response
     * @throws SpaceXIOException parsing/reading issues
     * @throws SpaceXMalformedUrl corrupt url
     */
    public <T> ApiResponse<T> tryGet(String url, Class<T> clazz) throws SpaceXIOException, SpaceXMalformedUrl {
        try {
            return apiClient.get(url, clazz);
        } catch (IOException e) {
            throw new SpaceXIOException("An error occured on fetching " + url);
        } catch (IllegalArgumentException e) {
            throw new SpaceXMalformedUrl("Url seems to be malformed: " + url);
        }
    }

    public ApiResponse<QueryResponse> tryPost(String url, String jsonBody) throws SpaceXIOException, SpaceXMalformedUrl {
        try {
            return apiClient.post(url, jsonBody, QueryResponse.class);
        } catch (IOException e) {
            throw new SpaceXIOException("An error occured on posting to " + url);
        } catch (IllegalArgumentException e) {
            throw new SpaceXMalformedUrl("Url seems to be malformed: " + url);
        }
    }

    public ApiResponse<Launch> getLaunchByID(String id) throws SpaceXIOException, SpaceXMalformedUrl {
        return tryGet("v5/launches/" + id, Launch.class);
    }

    public Launch[] getAllLaunches() throws SpaceXIOException, SpaceXMalformedUrl {
        String cacheKey = "launches_all.json";

        if (fileStorage.isValid(cacheKey)) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                return GSON.fromJson(cached, Launch[].class);
            }
        }

        try {
            ApiResponse<Launch[]> response = tryGet("v5/launches", Launch[].class);
            if (response.getError() != null) {
                throw new SpaceXIOException("API error: " + response.getError());
            }
            fileStorage.save(cacheKey, response.getRawBody());
            return response.getData();
        } catch (SpaceXIOException | SpaceXMalformedUrl e) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                printCacheWarning(cacheKey);
                return GSON.fromJson(cached, Launch[].class);
            }
            throw e;
        }
    }

    public Launch getLatestLaunch() throws SpaceXIOException, SpaceXMalformedUrl {
        String cacheKey = "launches_latest.json";

        if (fileStorage.isValid(cacheKey)) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                return GSON.fromJson(cached, Launch.class);
            }
        }

        try {
            ApiResponse<Launch> response = tryGet("v5/launches/latest", Launch.class);
            if (response.getError() != null) {
                throw new SpaceXIOException("API error: " + response.getError());
            }
            fileStorage.save(cacheKey, response.getRawBody());
            return response.getData();
        } catch (SpaceXIOException | SpaceXMalformedUrl e) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                printCacheWarning(cacheKey);
                return GSON.fromJson(cached, Launch.class);
            }
            throw e;
        }
    }

    public QueryResponse searchByDateRange(String start, String end) throws SpaceXIOException, SpaceXMalformedUrl {
        String cacheKey = "query_" + start + "_" + end + ".json";

        if (fileStorage.isValid(cacheKey)) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                return GSON.fromJson(cached, QueryResponse.class);
            }
        }

        String body = JsonBuilder.buildDateRangeQuery(start, end);
        try {
            ApiResponse<QueryResponse> response = tryPost("v5/launches/query", body);
            if (response.getError() != null) {
                throw new SpaceXIOException("API error: " + response.getError());
            }
            fileStorage.save(cacheKey, response.getRawBody());
            return response.getData();
        } catch (SpaceXIOException | SpaceXMalformedUrl e) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                printCacheWarning(cacheKey);
                return GSON.fromJson(cached, QueryResponse.class);
            }
            throw e;
        }
    }

    public QueryResponse getSuccessfulLaunches() throws SpaceXIOException, SpaceXMalformedUrl {
        String cacheKey = "query_success_true.json";

        if (fileStorage.isValid(cacheKey)) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                return GSON.fromJson(cached, QueryResponse.class);
            }
        }

        String body = JsonBuilder.buildSuccessQuery(true);
        try {
            ApiResponse<QueryResponse> response = tryPost("v5/launches/query", body);
            if (response.getError() != null) {
                throw new SpaceXIOException("API error: " + response.getError());
            }
            fileStorage.save(cacheKey, response.getRawBody());
            return response.getData();
        } catch (SpaceXIOException | SpaceXMalformedUrl e) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                printCacheWarning(cacheKey);
                return GSON.fromJson(cached, QueryResponse.class);
            }
            throw e;
        }
    }

    public QueryResponse getFailedLaunches() throws SpaceXIOException, SpaceXMalformedUrl {
        String cacheKey = "query_success_false.json";

        if (fileStorage.isValid(cacheKey)) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                return GSON.fromJson(cached, QueryResponse.class);
            }
        }

        String body = JsonBuilder.buildSuccessQuery(false);
        try {
            ApiResponse<QueryResponse> response = tryPost("v5/launches/query", body);
            if (response.getError() != null) {
                throw new SpaceXIOException("API error: " + response.getError());
            }
            fileStorage.save(cacheKey, response.getRawBody());
            return response.getData();
        } catch (SpaceXIOException | SpaceXMalformedUrl e) {
            String cached = fileStorage.load(cacheKey);
            if (cached != null) {
                printCacheWarning(cacheKey);
                return GSON.fromJson(cached, QueryResponse.class);
            }
            throw e;
        }
    }

    private void printCacheWarning(String cacheKey) {
        Instant ts = fileStorage.getTimestamp(cacheKey);
        String formatted = ts != null ? TIMESTAMP_FMT.format(ts) : "неизвестно";
        System.out.println("[!] Сервер недоступен. Показаны данные из кеша (сохранены " + formatted + ")");
    }
}
