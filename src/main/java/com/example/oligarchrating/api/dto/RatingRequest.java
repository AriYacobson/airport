package com.example.oligarchrating.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to rate a person against the oligarch threshold")
public record RatingRequest(
        @NotNull
        @Positive
        @Schema(example = "123456789")
        Long id,

        @NotNull
        @Valid
        PersonInformation personInformation,

        @NotNull
        @Valid
        FinancialAssets financialAssets) {
}
