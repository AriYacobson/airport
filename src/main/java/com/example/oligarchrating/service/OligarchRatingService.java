package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.api.dto.RatingResponse;
import com.example.oligarchrating.client.OligarchHelperClient;
import com.example.oligarchrating.domain.Oligarch;
import com.example.oligarchrating.mapper.OligarchMapper;
import com.example.oligarchrating.repository.OligarchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OligarchRatingService {

    private final AssetsEvaluator assetsEvaluator;
    private final OligarchHelperClient oligarchHelperClient;
    private final OligarchRepository oligarchRepository;
    private final OligarchMapper oligarchMapper;
    private final Clock clock;

    @Transactional
    public RatingResponse rate(RatingRequest request) {
        log.info("Rating person id={} {} {}",
                request.id(),
                request.personInformation().firstName(),
                request.personInformation().lastName());

        BigDecimal assetsValue = assetsEvaluator.evaluateInUsd(request.financialAssets());
        BigDecimal threshold = oligarchHelperClient.getOligarchThreshold();
        boolean isOligarch = assetsValue.compareTo(threshold) > 0;

        if (isOligarch) {
            persistOligarch(request, assetsValue);
        } else {
            log.info("Person id={} not an oligarch (assets={} threshold={})",
                    request.id(), assetsValue, threshold);
        }

        return oligarchMapper.toResponse(request, assetsValue, isOligarch);
    }

    private void persistOligarch(RatingRequest request, BigDecimal assetsValue) {
        Instant now = clock.instant();
        Oligarch entity = oligarchRepository.findById(request.id())
                .map(existing -> {
                    Oligarch updated = Oligarch.builder()
                            .id(existing.getId())
                            .firstName(request.personInformation().firstName())
                            .lastName(request.personInformation().lastName())
                            .assetsValue(assetsValue)
                            .createdAt(existing.getCreatedAt())
                            .updatedAt(now)
                            .version(existing.getVersion())
                            .build();
                    log.info("Updating existing oligarch id={}", existing.getId());
                    return updated;
                })
                .orElseGet(() -> {
                    log.info("Persisting new oligarch id={} assets={}", request.id(), assetsValue);
                    return oligarchMapper.toEntity(request, assetsValue, now);
                });
        oligarchRepository.save(entity);
    }
}
