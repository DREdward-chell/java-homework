package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LaunchSerializationTest {

    private final Gson gson = new Gson();

    @Test
    public void serializeSingleLaunch() {
        Launch launch = Launch.builder()
                .id("abc123")
                .name("TestMission")
                .flightNumber(42)
                .dateUtc("2022-01-01T00:00:00.000Z")
                .success(true)
                .build();

        String json = gson.toJson(launch);

        assertTrue(json.contains("flight_number"));
        assertTrue(json.contains("date_utc"));
        assertTrue(json.contains("TestMission"));
        assertFalse(json.contains("flightNumber"));
        assertFalse(json.contains("dateUtc"));
    }

    @Test
    public void serializeLaunchList() {
        List<Launch> launches = List.of(
                Launch.builder().name("A").flightNumber(1).build(),
                Launch.builder().name("B").flightNumber(2).build()
        );

        String json = gson.toJson(launches);

        JsonElement element = JsonParser.parseString(json);
        assertTrue(element.isJsonArray());
        JsonArray array = element.getAsJsonArray();
        assertEquals(2, array.size());
    }

    @Test
    public void serializeNullFields() {
        Launch launch = Launch.builder()
                .name("NullTest")
                .success(null)
                .details(null)
                .build();

        assertDoesNotThrow(() -> gson.toJson(launch));
    }

    @Test
    public void serializeThenDeserializeRoundTrip() {
        Launch original = Launch.builder()
                .id("roundtrip-id")
                .name("RoundTrip")
                .flightNumber(99)
                .dateUtc("2023-06-15T12:00:00.000Z")
                .success(true)
                .upcoming(false)
                .details("Round trip test")
                .build();

        String json = gson.toJson(original);
        Launch deserialized = gson.fromJson(json, Launch.class);

        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getFlightNumber(), deserialized.getFlightNumber());
        assertEquals(original.getDateUtc(), deserialized.getDateUtc());
        assertEquals(original.getSuccess(), deserialized.getSuccess());
        assertEquals(original.getDetails(), deserialized.getDetails());
    }
}
