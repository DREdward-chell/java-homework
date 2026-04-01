package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApiResponseTest {

    @Mock
    private HttpURLConnection connection;

    private static final String LAUNCH_JSON = "{\"id\":\"abc\",\"name\":\"TestMission\",\"flight_number\":1," +
            "\"date_utc\":\"2022-01-01T00:00:00.000Z\",\"success\":true,\"upcoming\":false}";

    @Test
    public void fromConnectionReturns200WithParsedBody() throws IOException {
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getResponseMessage()).thenReturn("OK");
        when(connection.getInputStream()).thenReturn(
                new ByteArrayInputStream(LAUNCH_JSON.getBytes(StandardCharsets.UTF_8)));

        ApiResponse<Launch> response = ApiResponse.fromConnection(connection, Launch.class);

        assertEquals(200, response.getStatus());
        assertNull(response.getError());
        assertNotNull(response.getData());
        assertEquals("TestMission", response.getData().getName());
    }

    @Test
    public void fromConnectionReturns404WithError() throws IOException {
        when(connection.getResponseCode()).thenReturn(404);
        when(connection.getResponseMessage()).thenReturn("Not Found");

        ApiResponse<Launch> response = ApiResponse.fromConnection(connection, Launch.class);

        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertNull(response.getData());
    }

    @Test
    public void fromConnectionSetsTimestamp() throws IOException {
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getResponseMessage()).thenReturn("OK");
        when(connection.getInputStream()).thenReturn(
                new ByteArrayInputStream(LAUNCH_JSON.getBytes(StandardCharsets.UTF_8)));

        Instant before = Instant.now();
        ApiResponse<Launch> response = ApiResponse.fromConnection(connection, Launch.class);
        Instant after = Instant.now();

        assertNotNull(response.getTimestamp());
        assertTrue(!response.getTimestamp().isBefore(before));
        assertTrue(!response.getTimestamp().isAfter(after.plus(Duration.ofSeconds(1))));
    }

    @Test
    public void fromConnectionStoresRawBody() throws IOException {
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getResponseMessage()).thenReturn("OK");
        when(connection.getInputStream()).thenReturn(
                new ByteArrayInputStream(LAUNCH_JSON.getBytes(StandardCharsets.UTF_8)));

        ApiResponse<Launch> response = ApiResponse.fromConnection(connection, Launch.class);

        assertNotNull(response.getRawBody());
        assertTrue(response.getRawBody().contains("TestMission"));
    }
}
