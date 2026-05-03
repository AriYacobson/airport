package com.example.oligarchrating.client;

import com.example.oligarchrating.config.ExternalServicesProperties;
import com.example.oligarchrating.config.RestClientConfig;
import com.example.oligarchrating.exception.ExternalServiceException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Currency;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetsValuationClientTest {

    private WireMockServer wireMock;
    private AssetsValuationClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        var props = new ExternalServicesProperties(
                new ExternalServicesProperties.ServiceProperties(
                        URI.create("http://localhost:" + wireMock.port()),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(2)),
                new ExternalServicesProperties.ServiceProperties(
                        URI.create("http://localhost:" + wireMock.port()),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(2)));
        var restClient = new RestClientConfig().assetsValuationRestClient(props);
        client = new AssetsValuationClient(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void evaluatesCashInUsd() {
        wireMock.stubFor(get(urlPathEqualTo("/assets-valuation/cash/evaluate"))
                .withQueryParam("amount", equalTo("16000000000"))
                .withQueryParam("localCurrency", equalTo("ILS"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valueUsd\": 4000000000.55}")));

        BigDecimal result = client.evaluateCashInUsd(
                new BigDecimal("16000000000"), Currency.getInstance("ILS"));

        assertThat(result).isEqualByComparingTo("4000000000.55");
    }

    @Test
    void getsBitcoinValueInUsd() {
        wireMock.stubFor(get(urlPathEqualTo("/assets-valuation/bitcoin/value"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valueUsd\": 60000.10}")));

        BigDecimal result = client.getBitcoinValueInUsd();

        assertThat(result).isEqualByComparingTo("60000.10");
    }

    @Test
    void wrapsServerErrorInExternalServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/assets-valuation/bitcoin/value"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.getBitcoinValueInUsd())
                .isInstanceOf(ExternalServiceException.class)
                .extracting("service").isEqualTo("assets-valuation");
    }

    @Test
    void throwsWhenBodyMissingValue() {
        wireMock.stubFor(get(urlPathEqualTo("/assets-valuation/bitcoin/value"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        assertThatThrownBy(() -> client.getBitcoinValueInUsd())
                .isInstanceOf(ExternalServiceException.class);
    }
}
