package com.edwards.SpaceXTracker.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for building JSON request bodies for the SpaceX API
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonBuilder {

    private static final Gson GSON = new Gson();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * Builds a POST /v5/launches/query body filtering by UTC date range.
     * Resulting JSON: {"query":{"date_utc":{"$gte":"<start>","$lte":"<end>"}},"options":{"limit":1000}}
     */
    public static String buildDateRangeQuery(String startDate, String endDate) {
        JsonObject dateRange = new JsonObject();
        dateRange.addProperty("$gte", startDate);
        dateRange.addProperty("$lte", endDate);

        JsonObject query = new JsonObject();
        query.add("date_utc", dateRange);

        JsonObject options = new JsonObject();
        options.addProperty("limit", 1000);

        JsonObject body = new JsonObject();
        body.add("query", query);
        body.add("options", options);

        return GSON.toJson(body);
    }

    /**
     * Builds a POST /v5/launches/query body filtering by success status.
     * Resulting JSON: {"query":{"success":<success>},"options":{"limit":1000}}
     */
    public static String buildSuccessQuery(boolean success) {
        JsonObject query = new JsonObject();
        query.addProperty("success", success);

        JsonObject options = new JsonObject();
        options.addProperty("limit", 1000);

        JsonObject body = new JsonObject();
        body.add("query", query);
        body.add("options", options);

        return GSON.toJson(body);
    }
}
