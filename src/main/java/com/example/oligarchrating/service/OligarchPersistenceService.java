package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.domain.Oligarch;
import com.example.oligarchrating.mapper.OligarchMapper;
import com.example.oligarchrating.repository.OligarchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OligarchPersistenceService {

    private final OligarchRepository oligarchRepository;
    private final OligarchMapper oligarchMapper;
    private final Clock clock;

    @Transactional
    public void upsert(RatingRequest request, BigDecimal assetsValue) {
        Instant now = clock.instant();
        Oligarch entity = oligarchRepository.findById(request.id())
                .map(existing -> {
                    existing.setFirstName(request.personInformation().firstName());
                    existing.setLastName(request.personInformation().lastName());
                    existing.setAssetsValue(assetsValue);
                    existing.setUpdatedAt(now);
                    log.info("Updating existing oligarch id={}", existing.getId());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("Persisting new oligarch id={}", request.id());
                    return oligarchMapper.toEntity(request, assetsValue, now);
                });
        oligarchRepository.save(entity);
    }
}
