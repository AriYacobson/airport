package com.example.oligarchrating.mapper;

import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.api.dto.RatingResponse;
import com.example.oligarchrating.domain.Oligarch;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class OligarchMapper {

    public Oligarch toEntity(RatingRequest request, BigDecimal assetsValue, Instant now) {
        return Oligarch.builder()
                .id(request.id())
                .firstName(request.personInformation().firstName())
                .lastName(request.personInformation().lastName())
                .assetsValue(assetsValue)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public RatingResponse toResponse(RatingRequest request, BigDecimal assetsValue, boolean oligarch) {
        return new RatingResponse(
                request.id(),
                request.personInformation().firstName(),
                request.personInformation().lastName(),
                assetsValue,
                oligarch);
    }
}
