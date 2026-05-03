package com.example.oligarchrating;

import com.example.oligarchrating.config.ExternalServicesProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OligarchRatingApplication {

    public static void main(String[] args) {
        SpringApplication.run(OligarchRatingApplication.class, args);
    }
}
