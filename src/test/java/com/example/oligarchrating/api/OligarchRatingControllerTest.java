package com.example.oligarchrating.api;

import com.example.oligarchrating.api.dto.RatingResponse;
import com.example.oligarchrating.api.error.GlobalExceptionHandler;
import com.example.oligarchrating.exception.ExternalServiceException;
import com.example.oligarchrating.service.OligarchRatingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OligarchRatingController.class)
@Import({GlobalExceptionHandler.class, OligarchRatingControllerTest.TestClockConfig.class})
class OligarchRatingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OligarchRatingService service;

    private static final String VALID_PAYLOAD = """
            {
              "id": 123456789,
              "personInformation": {"firstName": "Bill", "lastName": "Gates"},
              "financialAssets": {"cashAmount": 16000000000, "currency": "ILS", "bitcoinAmount": 50}
            }
            """;

    @Test
    void returnsRatingResponseForValidRequest() throws Exception {
        when(service.rate(any())).thenReturn(
                new RatingResponse(123456789L, "Bill", "Gates", new BigDecimal("17000000000.00"), true));

        mockMvc.perform(post("/api/v1/oligarch-ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123456789))
                .andExpect(jsonPath("$.firstName").value("Bill"))
                .andExpect(jsonPath("$.lastName").value("Gates"))
                .andExpect(jsonPath("$.assetsValue").value(17000000000.00))
                .andExpect(jsonPath("$.oligarch").value(true));
    }

    @Test
    void returns400WhenFirstNameMissing() throws Exception {
        String payload = """
                {
                  "id": 1,
                  "personInformation": {"firstName": "", "lastName": "Gates"},
                  "financialAssets": {"cashAmount": 1, "currency": "USD", "bitcoinAmount": 0}
                }
                """;
        mockMvc.perform(post("/api/v1/oligarch-ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.violations[0].field").value("personInformation.firstName"));
    }

    @Test
    void returns400WhenCashAmountNegative() throws Exception {
        String payload = """
                {
                  "id": 1,
                  "personInformation": {"firstName": "A", "lastName": "B"},
                  "financialAssets": {"cashAmount": -1, "currency": "USD", "bitcoinAmount": 0}
                }
                """;
        mockMvc.perform(post("/api/v1/oligarch-ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenBodyMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/oligarch-ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void returns502WhenUpstreamReturnsError() throws Exception {
        when(service.rate(any())).thenThrow(new ExternalServiceException("assets-valuation", "boom"));

        mockMvc.perform(post("/api/v1/oligarch-ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void returns504WhenUpstreamUnreachable() throws Exception {
        when(service.rate(any())).thenThrow(new ExternalServiceException(
                "assets-valuation", "timeout", new ResourceAccessException("connect timed out")));

        mockMvc.perform(post("/api/v1/oligarch-ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504));
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestClockConfig {
        @org.springframework.context.annotation.Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
