package com.example.oligarchrating.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient assetsValuationRestClient(ExternalServicesProperties properties) {
        return buildClient(properties.assetsValuation());
    }

    @Bean
    public RestClient oligarchHelperRestClient(ExternalServicesProperties properties) {
        return buildClient(properties.oligarchHelper());
    }

    private RestClient buildClient(ExternalServicesProperties.ServiceProperties props) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(props.connectTimeout())
                .withReadTimeout(props.readTimeout());
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        return RestClient.builder()
                .baseUrl(props.baseUrl().toString())
                .requestFactory(factory)
                .build();
    }
}
