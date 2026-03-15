package com.edwards.SpaceXTracker;

import com.edwards.SpaceXTracker.cli.SpaceXCommandLine;
import com.edwards.SpaceXTracker.config.ApiConfiguration;
import com.edwards.SpaceXTracker.exceptions.BaseSpaceXAppException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApiConfiguration.class)
public class SpaceXTrackerApplication {
	static void main(String[] args) {
		var app = SpringApplication.run(SpaceXTrackerApplication.class, args);
		try {
			app.getBean(SpaceXCommandLine.class).call();
		} catch (BaseSpaceXAppException e) {
			e.easyExit();
		}
	}
}