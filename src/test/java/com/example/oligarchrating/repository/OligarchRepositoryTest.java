package com.example.oligarchrating.repository;

import com.example.oligarchrating.domain.Oligarch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
class OligarchRepositoryTest {

    @Autowired
    OligarchRepository oligarchRepository;

    @Test
    void persistsAndLoadsOligarch() {
        Instant now = Instant.parse("2024-02-01T10:00:00Z");
        Oligarch saved = oligarchRepository.save(Oligarch.builder()
                .id(7L)
                .firstName("Mark")
                .lastName("Zuckerberg")
                .assetsValue(new BigDecimal("180000000000.00"))
                .createdAt(now)
                .updatedAt(now)
                .build());

        assertThat(oligarchRepository.findById(7L))
                .isPresent()
                .hasValueSatisfying(o -> assertThat(o.getAssetsValue())
                        .isEqualByComparingTo(saved.getAssetsValue()));
    }
}
