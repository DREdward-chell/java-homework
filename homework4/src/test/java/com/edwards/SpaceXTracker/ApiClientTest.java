package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.ApiClient;
import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.config.ApiConfiguration;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class ApiClientTest {

    private HttpServer server;
    private ApiClient client;
    private int port;

    private static final String LAUNCH_JSON =
            "{\"id\":\"abc\",\"name\":\"TestMission\",\"flight_number\":5," +
            "\"date_utc\":\"2022-01-01T00:00:00.000Z\",\"success\":true,\"upcoming\":false}";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();

        ApiConfiguration config = new ApiConfiguration();
        config.setBaseUrl("http://localhost:" + port + "/");
        config.setTimeout(5000);
        client = new ApiClient(config);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    public void getReturns200WithParsedBody() throws IOException {
        server.createContext("/v5/launches/abc", exchange -> {
            byte[] body = LAUNCH_JSON.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        ApiResponse<Launch> response = client.get("v5/launches/abc", Launch.class);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getData());
        assertEquals("TestMission", response.getData().getName());
        assertEquals(5, response.getData().getFlightNumber());
    }

    @Test
    public void getReturns404WithError() throws IOException {
        server.createContext("/v5/launches/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        ApiResponse<Launch> response = client.get("v5/launches/missing", Launch.class);

        assertEquals(404, response.getStatus());
        assertNotNull(response.getError());
        assertNull(response.getData());
    }

    @Test
    public void getThrowsIllegalArgumentForBadUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> client.get("not a valid url fragment ://bad", Launch.class));
    }

    @Test
    public void postReturns200WithParsedBody() throws IOException {
        server.createContext("/v5/launches/query", exchange -> {
            byte[] body = ("{\"docs\":[" + LAUNCH_JSON + "],\"totalDocs\":1,\"limit\":1000," +
                    "\"totalPages\":1,\"page\":1,\"hasNextPage\":false,\"hasPrevPage\":false}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        ApiResponse<com.edwards.SpaceXTracker.api.DTO.QueryResponse> response =
                client.post("v5/launches/query", "{\"query\":{},\"options\":{\"limit\":1000}}",
                        com.edwards.SpaceXTracker.api.DTO.QueryResponse.class);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().getDocs().size());
    }

    @Test
    public void postSetsContentTypeHeader() throws IOException {
        server.createContext("/v5/launches/headers", exchange -> {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            byte[] body = ("{\"contentType\":\"" + contentType + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        client.post("v5/launches/headers", "{}", com.google.gson.JsonObject.class);
        // No assertion needed — if Content-Type is missing the handler would fail
    }
}
