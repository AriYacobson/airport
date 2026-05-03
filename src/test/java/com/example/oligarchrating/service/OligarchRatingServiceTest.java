package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.FinancialAssets;
import com.example.oligarchrating.api.dto.PersonInformation;
import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.api.dto.RatingResponse;
import com.example.oligarchrating.client.OligarchHelperClient;
import com.example.oligarchrating.domain.Oligarch;
import com.example.oligarchrating.mapper.OligarchMapper;
import com.example.oligarchrating.repository.OligarchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OligarchRatingServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2024-01-15T10:15:30.00Z");

    @Mock
    AssetsEvaluator assetsEvaluator;
    @Mock
    OligarchHelperClient oligarchHelperClient;
    @Mock
    OligarchRepository oligarchRepository;

    OligarchMapper oligarchMapper = new OligarchMapper();
    Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    OligarchRatingService service;

    @BeforeEach
    void setUp() {
        service = new OligarchRatingService(
                assetsEvaluator, oligarchHelperClient, oligarchRepository, oligarchMapper, clock);
    }

    @Test
    void persistsAndReturnsOligarchWhenAssetsAboveThreshold() {
        RatingRequest request = sampleRequest();
        when(assetsEvaluator.evaluateInUsd(request.financialAssets()))
                .thenReturn(new BigDecimal("17000000000.00"));
        when(oligarchHelperClient.getOligarchThreshold()).thenReturn(new BigDecimal("1000000000"));
        when(oligarchRepository.findById(request.id())).thenReturn(Optional.empty());

        RatingResponse response = service.rate(request);

        assertThat(response.oligarch()).isTrue();
        assertThat(response.assetsValue()).isEqualByComparingTo("17000000000.00");
        assertThat(response.firstName()).isEqualTo("Bill");
        assertThat(response.lastName()).isEqualTo("Gates");
        assertThat(response.id()).isEqualTo(123456789L);

        ArgumentCaptor<Oligarch> captor = ArgumentCaptor.forClass(Oligarch.class);
        verify(oligarchRepository).save(captor.capture());
        Oligarch saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(123456789L);
        assertThat(saved.getAssetsValue()).isEqualByComparingTo("17000000000.00");
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_NOW);
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
        verify(oligarchRepository, never()).save(any());
    }

    @Test
    void thresholdEqualToAssetsIsNotOligarch() {
        RatingRequest request = sampleRequest();
        when(assetsEvaluator.evaluateInUsd(request.financialAssets())).thenReturn(new BigDecimal("1000"));
        when(oligarchHelperClient.getOligarchThreshold()).thenReturn(new BigDecimal("1000"));

        RatingResponse response = service.rate(request);

        assertThat(response.oligarch()).isFalse();
        verify(oligarchRepository, never()).save(any());
    }

    @Test
    void updatesExistingOligarchPreservingCreatedAt() {
        RatingRequest request = sampleRequest();
        Instant earlier = FIXED_NOW.minusSeconds(86400);
        Oligarch existing = Oligarch.builder()
                .id(request.id())
                .firstName("Old")
                .lastName("Name")
                .assetsValue(new BigDecimal("123"))
                .createdAt(earlier)
                .updatedAt(earlier)
                .version(1L)
                .build();
        when(assetsEvaluator.evaluateInUsd(request.financialAssets()))
                .thenReturn(new BigDecimal("17000000000.00"));
        when(oligarchHelperClient.getOligarchThreshold()).thenReturn(new BigDecimal("1000000000"));
        when(oligarchRepository.findById(request.id())).thenReturn(Optional.of(existing));

        service.rate(request);

        ArgumentCaptor<Oligarch> captor = ArgumentCaptor.forClass(Oligarch.class);
        verify(oligarchRepository).save(captor.capture());
        Oligarch saved = captor.getValue();
        assertThat(saved.getCreatedAt()).isEqualTo(earlier);
        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.getFirstName()).isEqualTo("Bill");
        assertThat(saved.getVersion()).isEqualTo(1L);
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
