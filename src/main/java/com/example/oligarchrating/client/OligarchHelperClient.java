package com.example.oligarchrating.client;

import com.example.oligarchrating.client.dto.ThresholdResponse;
import com.example.oligarchrating.exception.ExternalServiceException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Slf4j
@Component
public class OligarchHelperClient {

    public static final String SERVICE_NAME = "oligarch-helper";

    private final RestClient restClient;

    public OligarchHelperClient(RestClient oligarchHelperRestClient) {
        this.restClient = oligarchHelperRestClient;
    }

    @Retry(name = SERVICE_NAME)
    public BigDecimal getOligarchThreshold() {
        log.debug("Calling oligarch-helper oligarch-threshold");
        try {
            ThresholdResponse response = restClient.get()
                    .uri("/oligarch-helper/oligarch-threshold")
                    .retrieve()
                    .body(ThresholdResponse.class);
            if (response == null || response.threshold() == null) {
                throw new ExternalServiceException(SERVICE_NAME,
                        "oligarch-helper returned an empty threshold");
            }
            return response.threshold();
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new ExternalServiceException(SERVICE_NAME,
                    "oligarch-helper oligarch-threshold call failed: " + e.getMessage(), e);
        }
    }
}
