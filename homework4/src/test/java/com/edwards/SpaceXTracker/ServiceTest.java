package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.configurations.ServiceTestConfiguration;
import com.edwards.SpaceXTracker.exceptions.SpaceXIOException;
import com.edwards.SpaceXTracker.exceptions.SpaceXMalformedUrl;
import com.edwards.SpaceXTracker.services.SpaceXService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ServiceTestConfiguration.class})
public class ServiceTest {
    @Autowired
    private SpaceXService spaceXService;

    /**
     * Check if service can fetch basic {@link Launch}
     * @throws SpaceXIOException (if service is ok it shouldn't)
     * @see <a href="https://github.com/r-spacex/SpaceX-API/blob/master/docs/launches/v5/one.md">Official SpaceX api v5 example (id at the bottom)</a>
     */
    @Test
    public void testService() throws SpaceXIOException, SpaceXMalformedUrl {
        ApiResponse<Launch> response = spaceXService.getLaunchByID("5eb87d42ffd86e000604b384");
        assertEquals(200, response.getStatus());
        assertInstanceOf(Launch.class, response.getData());
        System.out.println(response.getData());
    }
}
