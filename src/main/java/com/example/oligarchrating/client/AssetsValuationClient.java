package com.example.oligarchrating.client;

import com.example.oligarchrating.client.dto.BitcoinValueResponse;
import com.example.oligarchrating.client.dto.CashEvaluationResponse;
import com.example.oligarchrating.exception.ExternalServiceException;
import com.example.oligarchrating.exception.RetryableExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Currency;

@Slf4j
@Component
public class AssetsValuationClient {

    public static final String SERVICE_NAME = "assets-valuation";

    private final RestClient restClient;

    public AssetsValuationClient(RestClient assetsValuationRestClient) {
        this.restClient = assetsValuationRestClient;
    }

    @CircuitBreaker(name = SERVICE_NAME)
    @Retry(name = SERVICE_NAME)
    public BigDecimal evaluateCashInUsd(BigDecimal amount, Currency localCurrency) {
        log.debug("Calling assets-valuation cash/evaluate amount={} currency={}", amount, localCurrency);
        try {
            CashEvaluationResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/assets-valuation/cash/evaluate")
                            .queryParam("amount", amount.toPlainString())
                            .queryParam("localCurrency", localCurrency.getCurrencyCode())
                            .build())
                    .retrieve()
                    .body(CashEvaluationResponse.class);
            return requireValue(response == null ? null : response.valueUsd(), "cash/evaluate");
        } catch (HttpClientErrorException e) {
            throw new ExternalServiceException(SERVICE_NAME,
                    "assets-valuation cash/evaluate rejected: " + e.getStatusCode(), e);
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new RetryableExternalServiceException(SERVICE_NAME,
                    "assets-valuation cash/evaluate call failed: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = SERVICE_NAME)
    @Retry(name = SERVICE_NAME)
    public BigDecimal getBitcoinValueInUsd() {
        log.debug("Calling assets-valuation bitcoin/value");
        try {
            BitcoinValueResponse response = restClient.get()
                    .uri("/assets-valuation/bitcoin/value")
                    .retrieve()
                    .body(BitcoinValueResponse.class);
            return requireValue(response == null ? null : response.valueUsd(), "bitcoin/value");
        } catch (HttpClientErrorException e) {
            throw new ExternalServiceException(SERVICE_NAME,
                    "assets-valuation bitcoin/value rejected: " + e.getStatusCode(), e);
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new RetryableExternalServiceException(SERVICE_NAME,
                    "assets-valuation bitcoin/value call failed: " + e.getMessage(), e);
        }
    }

    private BigDecimal requireValue(BigDecimal value, String endpoint) {
        if (value == null) {
            throw new ExternalServiceException(SERVICE_NAME,
                    "assets-valuation " + endpoint + " returned an empty body");
        }
        return value;
    }
}
