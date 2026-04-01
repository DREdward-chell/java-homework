package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.ApiMapping;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApiMappingTest {

    @Test
    public void isErrorReturnsTrueFor4xx() {
        assertTrue(ApiMapping.isError(400));
        assertTrue(ApiMapping.isError(404));
        assertTrue(ApiMapping.isError(499));
    }

    @Test
    public void isErrorReturnsTrueFor5xx() {
        assertTrue(ApiMapping.isError(500));
        assertTrue(ApiMapping.isError(503));
    }

    @Test
    public void isErrorReturnsFalseFor2xx() {
        assertFalse(ApiMapping.isError(200));
        assertFalse(ApiMapping.isError(201));
    }

    @Test
    public void isErrorReturnsFalseFor3xx() {
        assertFalse(ApiMapping.isError(301));
        assertFalse(ApiMapping.isError(302));
    }
}
