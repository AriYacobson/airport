package com.example.oligarchrating.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI oligarchRatingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Oligarch Rating API")
                .description("Rates persons against the world oligarch threshold")
                .version("v1")
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
