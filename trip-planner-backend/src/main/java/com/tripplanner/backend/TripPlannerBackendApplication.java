package com.tripplanner.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        exclude = { DataSourceAutoConfiguration.class }
)
public class TripPlannerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripPlannerBackendApplication.class, args);
    }
}
