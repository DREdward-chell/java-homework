package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.ApiClient;
import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.api.DTO.QueryResponse;
import com.edwards.SpaceXTracker.cache.FileStorage;
import com.edwards.SpaceXTracker.exceptions.SpaceXIOException;
import com.edwards.SpaceXTracker.exceptions.SpaceXMalformedUrl;
import com.edwards.SpaceXTracker.services.SpaceXService;
import com.edwards.SpaceXTracker.util.TestResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpaceXServiceTest {

    @Mock
    private ApiClient apiClient;

    @Mock
    private FileStorage fileStorage;

    private SpaceXService service;

    @BeforeEach
    void setUp() {
        service = new SpaceXService(apiClient, fileStorage);
    }

    @Test
    public void getAllLaunchesReturnsArray() throws Exception {
        String json = TestResourceLoader.loadResource("launch_array.json");
        ApiResponse<Launch[]> mockResponse = mockApiResponse(200, new Launch[]{
                Launch.builder().name("A").build(),
                Launch.builder().name("B").build()
        }, json);

        when(fileStorage.isValid("launches_all.json")).thenReturn(false);
        when(apiClient.get(eq("v5/launches"), eq(Launch[].class))).thenReturn(mockResponse);

        Launch[] result = service.getAllLaunches();

        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    public void getLatestLaunchReturnsSingleLaunch() throws Exception {
        String json = TestResourceLoader.loadResource("latest_launch.json");
        Launch launch = Launch.builder().name("Crew-4").flightNumber(159).build();
        ApiResponse<Launch> mockResponse = mockApiResponse(200, launch, json);

        when(fileStorage.isValid("launches_latest.json")).thenReturn(false);
        when(apiClient.get(eq("v5/launches/latest"), eq(Launch.class))).thenReturn(mockResponse);

        Launch result = service.getLatestLaunch();

        assertNotNull(result);
        assertEquals("Crew-4", result.getName());
    }

    @Test
    public void searchByDateRangeDelegatesToPost() throws Exception {
        String json = TestResourceLoader.loadResource("query_response.json");
        QueryResponse qr = new QueryResponse();
        ApiResponse<QueryResponse> mockResponse = mockApiResponse(200, qr, json);

        when(fileStorage.isValid(anyString())).thenReturn(false);
        when(apiClient.post(eq("v5/launches/query"), contains("date_utc"), eq(QueryResponse.class)))
                .thenReturn(mockResponse);

        service.searchByDateRange("2020-01-01", "2020-12-31");

        verify(apiClient).post(eq("v5/launches/query"), contains("date_utc"), eq(QueryResponse.class));
    }

    @Test
    public void getSuccessfulLaunchesSendsCorrectFilter() throws Exception {
        String json = TestResourceLoader.loadResource("query_response.json");
        ApiResponse<QueryResponse> mockResponse = mockApiResponse(200, new QueryResponse(), json);

        when(fileStorage.isValid("query_success_true.json")).thenReturn(false);
        when(apiClient.post(eq("v5/launches/query"), contains("\"success\":true"), eq(QueryResponse.class)))
                .thenReturn(mockResponse);

        service.getSuccessfulLaunches();

        verify(apiClient).post(eq("v5/launches/query"), contains("\"success\":true"), eq(QueryResponse.class));
    }

    @Test
    public void getFailedLaunchesSendsCorrectFilter() throws Exception {
        String json = TestResourceLoader.loadResource("query_response.json");
        ApiResponse<QueryResponse> mockResponse = mockApiResponse(200, new QueryResponse(), json);

        when(fileStorage.isValid("query_success_false.json")).thenReturn(false);
        when(apiClient.post(eq("v5/launches/query"), contains("\"success\":false"), eq(QueryResponse.class)))
                .thenReturn(mockResponse);

        service.getFailedLaunches();

        verify(apiClient).post(eq("v5/launches/query"), contains("\"success\":false"), eq(QueryResponse.class));
    }

    @Test
    public void tryGetWrapsIOExceptionAsSpaceXIOException() throws Exception {
        when(apiClient.get(anyString(), any())).thenThrow(new IOException("network error"));

        assertThrows(SpaceXIOException.class, () -> service.tryGet("v5/launches", Launch.class));
    }

    @Test
    public void tryGetWrapsIllegalArgumentAsSpaceXMalformedUrl() throws Exception {
        when(apiClient.get(anyString(), any())).thenThrow(new IllegalArgumentException("bad url"));

        assertThrows(SpaceXMalformedUrl.class, () -> service.tryGet("bad url", Launch.class));
    }

    @Test
    public void getAllLaunchesReturnsCachedDataWhenCacheValid() throws Exception {
        String json = TestResourceLoader.loadResource("launch_array.json");
        when(fileStorage.isValid("launches_all.json")).thenReturn(true);
        when(fileStorage.load("launches_all.json")).thenReturn(json);

        Launch[] result = service.getAllLaunches();

        assertNotNull(result);
        verify(apiClient, never()).get(anyString(), any());
    }

    @Test
    public void getAllLaunchesFallsToCacheOnNetworkError() throws Exception {
        String json = TestResourceLoader.loadResource("launch_array.json");
        when(fileStorage.isValid("launches_all.json")).thenReturn(false);
        when(apiClient.get(eq("v5/launches"), eq(Launch[].class))).thenThrow(new IOException("network error"));
        when(fileStorage.load("launches_all.json")).thenReturn(json);

        Launch[] result = service.getAllLaunches();

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    public void tryPostWrapsIOExceptionAsSpaceXIOException() throws Exception {
        when(apiClient.post(anyString(), anyString(), any())).thenThrow(new IOException("timeout"));

        assertThrows(SpaceXIOException.class, () -> service.tryPost("v5/launches/query", "{}"));
    }

    @Test
    public void tryPostWrapsIllegalArgumentAsSpaceXMalformedUrl() throws Exception {
        when(apiClient.post(anyString(), anyString(), any())).thenThrow(new IllegalArgumentException("bad"));

        assertThrows(SpaceXMalformedUrl.class, () -> service.tryPost("bad url", "{}"));
    }

    @Test
    public void getLatestLaunchFallsToCacheOnNetworkError() throws Exception {
        String json = TestResourceLoader.loadResource("latest_launch.json");
        when(fileStorage.isValid("launches_latest.json")).thenReturn(false);
        when(apiClient.get(eq("v5/launches/latest"), eq(Launch.class))).thenThrow(new IOException("down"));
        when(fileStorage.load("launches_latest.json")).thenReturn(json);

        Launch result = service.getLatestLaunch();

        assertNotNull(result);
        assertEquals("Crew-4", result.getName());
    }

    @Test
    public void searchByDateRangeFallsToCacheOnNetworkError() throws Exception {
        String json = TestResourceLoader.loadResource("query_response.json");
        when(fileStorage.isValid(anyString())).thenReturn(false);
        when(apiClient.post(anyString(), anyString(), eq(QueryResponse.class))).thenThrow(new IOException("down"));
        when(fileStorage.load("query_2020-01-01_2020-12-31.json")).thenReturn(json);

        QueryResponse result = service.searchByDateRange("2020-01-01", "2020-12-31");

        assertNotNull(result);
    }

    @Test
    public void getSuccessfulLaunchesFallsToCacheOnNetworkError() throws Exception {
        String json = TestResourceLoader.loadResource("query_response.json");
        when(fileStorage.isValid("query_success_true.json")).thenReturn(false);
        when(apiClient.post(anyString(), anyString(), eq(QueryResponse.class))).thenThrow(new IOException("down"));
        when(fileStorage.load("query_success_true.json")).thenReturn(json);

        QueryResponse result = service.getSuccessfulLaunches();

        assertNotNull(result);
    }

    @Test
    public void getFailedLaunchesFallsToCacheOnNetworkError() throws Exception {
        String json = TestResourceLoader.loadResource("query_response.json");
        when(fileStorage.isValid("query_success_false.json")).thenReturn(false);
        when(apiClient.post(anyString(), anyString(), eq(QueryResponse.class))).thenThrow(new IOException("down"));
        when(fileStorage.load("query_success_false.json")).thenReturn(json);

        QueryResponse result = service.getFailedLaunches();

        assertNotNull(result);
    }

    @Test
    public void getAllLaunchesThrowsWhenNoCacheAndNetworkFails() throws Exception {
        when(fileStorage.isValid("launches_all.json")).thenReturn(false);
        when(apiClient.get(eq("v5/launches"), eq(Launch[].class))).thenThrow(new IOException("down"));
        when(fileStorage.load("launches_all.json")).thenReturn(null);

        assertThrows(SpaceXIOException.class, () -> service.getAllLaunches());
    }

    @Test
    public void getLatestLaunchReturnsCachedDataWhenCacheValid() throws Exception {
        String json = TestResourceLoader.loadResource("latest_launch.json");
        when(fileStorage.isValid("launches_latest.json")).thenReturn(true);
        when(fileStorage.load("launches_latest.json")).thenReturn(json);

        Launch result = service.getLatestLaunch();

        assertNotNull(result);
        verify(apiClient, never()).get(anyString(), any());
    }

    @Test
    public void printCacheWarningWithTimestamp() throws Exception {
        String json = TestResourceLoader.loadResource("latest_launch.json");
        when(fileStorage.isValid("launches_latest.json")).thenReturn(false);
        when(apiClient.get(eq("v5/launches/latest"), eq(Launch.class))).thenThrow(new IOException("down"));
        when(fileStorage.load("launches_latest.json")).thenReturn(json);
        when(fileStorage.getTimestamp("launches_latest.json")).thenReturn(java.time.Instant.now());

        Launch result = service.getLatestLaunch();

        assertNotNull(result);
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> mockApiResponse(int status, T data, String rawBody) throws Exception {
        ApiResponse<T> resp = (ApiResponse<T>) org.mockito.Mockito.mock(ApiResponse.class,
                org.mockito.Mockito.withSettings().lenient());
        resp.getStatus(); // suppress unused warning — status not always checked
        org.mockito.Mockito.lenient().when(resp.getStatus()).thenReturn(status);
        org.mockito.Mockito.lenient().when(resp.getData()).thenReturn(data);
        org.mockito.Mockito.lenient().when(resp.getRawBody()).thenReturn(rawBody);
        org.mockito.Mockito.lenient().when(resp.getError()).thenReturn(null);
        return resp;
    }
}
