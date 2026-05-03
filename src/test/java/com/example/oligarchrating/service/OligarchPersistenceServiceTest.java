package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.FinancialAssets;
import com.example.oligarchrating.api.dto.PersonInformation;
import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.domain.Oligarch;
import com.example.oligarchrating.mapper.OligarchMapper;
import com.example.oligarchrating.repository.OligarchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OligarchPersistenceServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2024-01-15T10:15:30.00Z");

    @Mock
    OligarchRepository oligarchRepository;

    OligarchMapper oligarchMapper = new OligarchMapper();
    Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    OligarchPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new OligarchPersistenceService(oligarchRepository, oligarchMapper, clock);
    }

    @Test
    void insertsNewOligarch() {
        RatingRequest request = sampleRequest();
        BigDecimal assets = new BigDecimal("17000000000.00");
        when(oligarchRepository.findById(request.id())).thenReturn(Optional.empty());

        persistenceService.upsert(request, assets);

        ArgumentCaptor<Oligarch> captor = ArgumentCaptor.forClass(Oligarch.class);
        org.mockito.Mockito.verify(oligarchRepository).save(captor.capture());
        Oligarch saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(request.id());
        assertThat(saved.getAssetsValue()).isEqualByComparingTo(assets);
        assertThat(saved.getCreatedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_NOW);
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
        when(oligarchRepository.findById(request.id())).thenReturn(Optional.of(existing));

        persistenceService.upsert(request, new BigDecimal("17000000000.00"));

        ArgumentCaptor<Oligarch> captor = ArgumentCaptor.forClass(Oligarch.class);
        org.mockito.Mockito.verify(oligarchRepository).save(captor.capture());
        Oligarch saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getCreatedAt()).isEqualTo(earlier);
        assertThat(saved.getUpdatedAt()).isEqualTo(FIXED_NOW);
        assertThat(saved.getFirstName()).isEqualTo("Bill");
        assertThat(saved.getLastName()).isEqualTo("Gates");
        assertThat(saved.getAssetsValue()).isEqualByComparingTo("17000000000.00");
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
