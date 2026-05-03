package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.FinancialAssets;
import com.example.oligarchrating.api.dto.PersonInformation;
import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.api.dto.RatingResponse;
import com.example.oligarchrating.client.OligarchHelperClient;
import com.example.oligarchrating.mapper.OligarchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OligarchRatingServiceTest {

    @Mock
    AssetsEvaluator assetsEvaluator;
    @Mock
    OligarchHelperClient oligarchHelperClient;
    @Mock
    OligarchPersistenceService oligarchPersistenceService;

    OligarchMapper oligarchMapper = new OligarchMapper();

    OligarchRatingService service;

    @BeforeEach
    void setUp() {
        service = new OligarchRatingService(
                assetsEvaluator, oligarchHelperClient, oligarchPersistenceService, oligarchMapper);
    }

    @Test
    void persistsAndReturnsOligarchWhenAssetsAboveThreshold() {
        RatingRequest request = sampleRequest();
        when(assetsEvaluator.evaluateInUsd(request.financialAssets()))
                .thenReturn(new BigDecimal("17000000000.00"));
        when(oligarchHelperClient.getOligarchThreshold()).thenReturn(new BigDecimal("1000000000"));

        RatingResponse response = service.rate(request);

        assertThat(response.oligarch()).isTrue();
        assertThat(response.assetsValue()).isEqualByComparingTo("17000000000.00");
        assertThat(response.firstName()).isEqualTo("Bill");
        assertThat(response.lastName()).isEqualTo("Gates");
        assertThat(response.id()).isEqualTo(123456789L);

        verify(oligarchPersistenceService).upsert(request, new BigDecimal("17000000000.00"));
    }

    @Test
    void doesNotPersistWhenAssetsBelowThreshold() {
        RatingRequest request = sampleRequest();
        when(assetsEvaluator.evaluateInUsd(request.financialAssets()))
                .thenReturn(new BigDecimal("500"));
        when(oligarchHelperClient.getOligarchThreshold()).thenReturn(new BigDecimal("1000"));

        RatingResponse response = service.rate(request);

        assertThat(response.oligarch()).isFalse();
        assertThat(response.assetsValue()).isEqualByComparingTo("500");
        verify(oligarchPersistenceService, never()).upsert(any(), any());
    }

    @Test
    void thresholdEqualToAssetsIsNotOligarch() {
        RatingRequest request = sampleRequest();
        when(assetsEvaluator.evaluateInUsd(request.financialAssets())).thenReturn(new BigDecimal("1000"));
        when(oligarchHelperClient.getOligarchThreshold()).thenReturn(new BigDecimal("1000"));

        RatingResponse response = service.rate(request);

        assertThat(response.oligarch()).isFalse();
        verify(oligarchPersistenceService, never()).upsert(any(), any());
    }

    private RatingRequest sampleRequest() {
        return new RatingRequest(
                123456789L,
                new PersonInformation("Bill", "Gates"),
                new FinancialAssets(
                        new BigDecimal("16000000000"),
                        Currency.getInstance("ILS"),
                        new BigDecimal("50")));
    }
}
