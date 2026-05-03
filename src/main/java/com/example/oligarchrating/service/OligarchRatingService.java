package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.api.dto.RatingResponse;
import com.example.oligarchrating.client.OligarchHelperClient;
import com.example.oligarchrating.mapper.OligarchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OligarchRatingService {

    private final AssetsEvaluator assetsEvaluator;
    private final OligarchHelperClient oligarchHelperClient;
    private final OligarchPersistenceService oligarchPersistenceService;
    private final OligarchMapper oligarchMapper;

    public RatingResponse rate(RatingRequest request) {
        log.info("Rating person id={}", request.id());

        BigDecimal assetsValue = assetsEvaluator.evaluateInUsd(request.financialAssets());
        BigDecimal threshold = oligarchHelperClient.getOligarchThreshold();
        boolean isOligarch = assetsValue.compareTo(threshold) > 0;

        if (isOligarch) {
            oligarchPersistenceService.upsert(request, assetsValue);
        } else {
            log.info("Person id={} not an oligarch (assets={} threshold={})",
                    request.id(), assetsValue, threshold);
        }

        return oligarchMapper.toResponse(request, assetsValue, isOligarch);
    }
}
