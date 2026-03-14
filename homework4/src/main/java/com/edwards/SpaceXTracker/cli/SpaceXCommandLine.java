package com.edwards.SpaceXTracker.cli;

import com.edwards.SpaceXTracker.api.ApiResponse;
import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.exceptions.BaseSpaceXAppException;
import com.edwards.SpaceXTracker.services.SpaceXService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.concurrent.Callable;

@Component
@AllArgsConstructor
public class SpaceXCommandLine implements Callable<Integer> {
    SpaceXService service;

    @Override
    public Integer call() throws BaseSpaceXAppException {
        ApiResponse<Launch> resp = service.getLaunchByID("5eb87d42ffd86e000604b384");
        System.out.println(resp.getData());

        return 0;
    }
}
