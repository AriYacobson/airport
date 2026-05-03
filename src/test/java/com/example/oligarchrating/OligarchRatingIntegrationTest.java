package com.example.oligarchrating;

import com.example.oligarchrating.repository.OligarchRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:oligarch-it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "resilience4j.retry.instances.assets-valuation.max-attempts=1",
        "resilience4j.retry.instances.oligarch-helper.max-attempts=1"
})
class OligarchRatingIntegrationTest {

    private static final WireMockServer assetsValuation;
    private static final WireMockServer oligarchHelper;

    static {
        assetsValuation = new WireMockServer(wireMockConfig().dynamicPort());
        oligarchHelper = new WireMockServer(wireMockConfig().dynamicPort());
        assetsValuation.start();
        oligarchHelper.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            assetsValuation.stop();
            oligarchHelper.stop();
        }));
    }

    @Autowired
    OligarchRepository oligarchRepository;

    @Autowired
    TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("oligarch-rating.external.assets-valuation.base-url",
                () -> "http://localhost:" + assetsValuation.port());
        registry.add("oligarch-rating.external.oligarch-helper.base-url",
                () -> "http://localhost:" + oligarchHelper.port());
    }

    @BeforeEach
    void setUp() {
        assetsValuation.resetAll();
        oligarchHelper.resetAll();
        oligarchRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        oligarchRepository.deleteAll();
    }

    @Test
    void persistsOligarchAndReturnsRating() {
        stubCash("16000000000", "ILS", "4000000000");
        stubBitcoin("60000");
        stubThreshold("1000000000");

        ResponseEntity<Map> response = postRating(billGatesPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("id")).isEqualTo(123456789);
        assertThat(body.get("firstName")).isEqualTo("Bill");
        assertThat(body.get("lastName")).isEqualTo("Gates");
        assertThat(body.get("oligarch")).isEqualTo(true);
        assertThat(new BigDecimal(body.get("assetsValue").toString()))
                .isEqualByComparingTo("4003000000.00");

        assertThat(oligarchRepository.findById(123456789L))
                .isPresent()
                .hasValueSatisfying(o -> {
                    assertThat(o.getFirstName()).isEqualTo("Bill");
                    assertThat(o.getLastName()).isEqualTo("Gates");
                    assertThat(o.getAssetsValue()).isEqualByComparingTo(new BigDecimal("4003000000.00"));
                });
    }

    @Test
    void doesNotPersistWhenBelowThreshold() {
        stubCash("100", "USD", "100");
        stubBitcoin("60000");
        stubThreshold("1000000000");

        String payload = """
                {
                  "id": 42,
                  "personInformation": {"firstName": "Alice", "lastName": "Smith"},
                  "financialAssets": {"cashAmount": 100, "currency": "USD", "bitcoinAmount": 0}
                }
                """;

        ResponseEntity<Map> response = postRating(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("oligarch")).isEqualTo(false);
        assertThat(oligarchRepository.findById(42L)).isEmpty();
    }

    @Test
    void returns502WhenAssetsValuationFails() {
        assetsValuation.stubFor(get(urlPathEqualTo("/assets-valuation/cash/evaluate"))
                .willReturn(aResponse().withStatus(500)));
        stubBitcoin("60000");
        stubThreshold("1000000000");

        ResponseEntity<Map> response = postRating(billGatesPayload());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void returns400OnInvalidPayload() {
        String payload = """
                {
                  "id": -1,
                  "personInformation": {"firstName": "", "lastName": ""},
                  "financialAssets": {"cashAmount": -1, "currency": "USD", "bitcoinAmount": -1}
                }
                """;

        ResponseEntity<Map> response = postRating(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("violations")).isInstanceOf(java.util.List.class);
    }

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ResponseEntity<Map> postRating(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/api/v1/oligarch-ratings",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class);
    }

    private void stubCash(String amount, String currency, String valueUsd) {
        assetsValuation.stubFor(get(urlPathEqualTo("/assets-valuation/cash/evaluate"))
                .withQueryParam("amount", equalTo(amount))
                .withQueryParam("localCurrency", equalTo(currency))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valueUsd\": " + valueUsd + "}")));
    }

    private void stubBitcoin(String valueUsd) {
        assetsValuation.stubFor(get(urlPathEqualTo("/assets-valuation/bitcoin/value"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valueUsd\": " + valueUsd + "}")));
    }

    private void stubThreshold(String threshold) {
        oligarchHelper.stubFor(get(urlPathEqualTo("/oligarch-helper/oligarch-threshold"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"threshold\": " + threshold + "}")));
    }

    private String billGatesPayload() {
        return """
                {
                  "id": 123456789,
                  "personInformation": {"firstName": "Bill", "lastName": "Gates"},
                  "financialAssets": {"cashAmount": 16000000000, "currency": "ILS", "bitcoinAmount": 50}
                }
                """;
    }
}
