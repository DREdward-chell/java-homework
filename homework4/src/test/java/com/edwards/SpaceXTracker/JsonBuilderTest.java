package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.JsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonBuilderTest {

    @Test
    public void buildDateRangeQueryContainsCorrectStructure() {
        String json = JsonBuilder.buildDateRangeQuery("2020-01-01", "2020-12-31");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        JsonObject query = root.getAsJsonObject("query");
        assertNotNull(query);

        JsonObject dateUtc = query.getAsJsonObject("date_utc");
        assertNotNull(dateUtc);
        assertEquals("2020-01-01", dateUtc.get("$gte").getAsString());
        assertEquals("2020-12-31", dateUtc.get("$lte").getAsString());

        JsonObject options = root.getAsJsonObject("options");
        assertNotNull(options);
        assertEquals(1000, options.get("limit").getAsInt());
    }

    @Test
    public void buildSuccessQueryTrue() {
        String json = JsonBuilder.buildSuccessQuery(true);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject query = root.getAsJsonObject("query");

        assertTrue(query.get("success").getAsBoolean());
    }

    @Test
    public void buildSuccessQueryFalse() {
        String json = JsonBuilder.buildSuccessQuery(false);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject query = root.getAsJsonObject("query");

        assertFalse(query.get("success").getAsBoolean());
    }

    @Test
    public void toJsonSerializesObject() {
        record SimpleObj(String name, int value) {}
        String json = JsonBuilder.toJson(new SimpleObj("test", 42));

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("test", obj.get("name").getAsString());
        assertEquals(42, obj.get("value").getAsInt());
    }
}
