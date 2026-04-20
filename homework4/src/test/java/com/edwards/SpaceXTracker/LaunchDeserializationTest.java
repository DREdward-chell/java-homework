package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.DTO.Core;
import com.edwards.SpaceXTracker.api.DTO.Failure;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.util.TestResourceLoader;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LaunchDeserializationTest {

    private final Gson gson = new Gson();

    @Test
    public void deserializeSingleLaunch() {
        String json = TestResourceLoader.loadResource("single_launch.json");
        Launch launch = gson.fromJson(json, Launch.class);

        assertEquals("5eb87d42ffd86e000604b384", launch.getId());
        assertEquals("FalconSat", launch.getName());
        assertEquals(1, launch.getFlightNumber());
        assertEquals("2006-03-24T22:30:00.000Z", launch.getDateUtc());
        assertEquals(Boolean.FALSE, launch.getSuccess());
        assertFalse(launch.isUpcoming());
        assertEquals("Engine failure at 33 seconds", launch.getDetails());
    }

    @Test
    public void deserializeLaunchArray() {
        String json = TestResourceLoader.loadResource("launch_array.json");
        Launch[] launches = gson.fromJson(json, Launch[].class);

        assertEquals(3, launches.length);
        assertEquals("FalconSat", launches[0].getName());
        assertEquals("DemoSat", launches[1].getName());
        assertEquals("Trailblazer", launches[2].getName());
        assertNull(launches[2].getSuccess());
    }

    @Test
    public void deserializeNullFields() {
        String json = TestResourceLoader.loadResource("launch_null_fields.json");
        Launch launch = gson.fromJson(json, Launch.class);

        assertNull(launch.getSuccess());
        assertNull(launch.getDetails());
        assertNull(launch.getFailures());
        assertNotNull(launch.getCores());
        assertTrue(launch.getCores().isEmpty());
    }

    @Test
    public void deserializeFailures() {
        String json = TestResourceLoader.loadResource("single_launch.json");
        Launch launch = gson.fromJson(json, Launch.class);

        assertNotNull(launch.getFailures());
        assertEquals(1, launch.getFailures().size());
        Failure failure = launch.getFailures().get(0);
        assertEquals(33, failure.getTime());
        assertNull(failure.getAltitude());
        assertEquals("merlin engine failure", failure.getReason());
    }

    @Test
    public void deserializeCores() {
        String json = TestResourceLoader.loadResource("single_launch.json");
        Launch launch = gson.fromJson(json, Launch.class);

        assertNotNull(launch.getCores());
        assertEquals(1, launch.getCores().size());
        Core core = launch.getCores().get(0);
        assertEquals("5e9e289df35918033d3b2623", core.getCore());
        assertEquals(1, core.getFlight());
        assertEquals(Boolean.FALSE, core.getGridfins());
        assertEquals(Boolean.FALSE, core.getLegs());
        assertEquals(Boolean.FALSE, core.getReused());
        assertEquals(Boolean.FALSE, core.getLandingAttempt());
        assertNull(core.getLandingSuccess());
        assertNull(core.getLandingType());
        assertNull(core.getLandpad());
    }

    @Test
    public void deserializeInvalidJsonThrowsException() {
        String json = TestResourceLoader.loadResource("invalid.json");
        assertThrows(JsonSyntaxException.class, () -> gson.fromJson(json, Launch.class));
    }
}
