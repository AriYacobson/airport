package com.example.oligarchrating.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "oligarch-rating.external")
public record ExternalServicesProperties(
        @NotNull ServiceProperties assetsValuation,
        @NotNull ServiceProperties oligarchHelper) {

    public record ServiceProperties(
            @NotNull URI baseUrl,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout) {
    }
}
