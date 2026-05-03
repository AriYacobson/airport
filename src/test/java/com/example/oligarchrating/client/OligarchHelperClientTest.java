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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OligarchHelperClientTest {

    private WireMockServer wireMock;
    private OligarchHelperClient client;

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
        var restClient = new RestClientConfig().oligarchHelperRestClient(props);
        client = new OligarchHelperClient(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void returnsThreshold() {
        wireMock.stubFor(get(urlPathEqualTo("/oligarch-helper/oligarch-threshold"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"threshold\": 1000000000}")));

        BigDecimal threshold = client.getOligarchThreshold();

        assertThat(threshold).isEqualByComparingTo("1000000000");
    }

    @Test
    void wrapsErrorInExternalServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/oligarch-helper/oligarch-threshold"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.getOligarchThreshold())
                .isInstanceOf(ExternalServiceException.class)
                .extracting("service").isEqualTo("oligarch-helper");
    }

    @Test
    void throwsWhenBodyMissingThreshold() {
        wireMock.stubFor(get(urlPathEqualTo("/oligarch-helper/oligarch-threshold"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        assertThatThrownBy(() -> client.getOligarchThreshold())
                .isInstanceOf(ExternalServiceException.class)
                .extracting("service").isEqualTo("oligarch-helper");
    }
}
