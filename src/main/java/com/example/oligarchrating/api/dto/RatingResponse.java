package com.example.oligarchrating.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Result of evaluating a person against the oligarch threshold")
public record RatingResponse(
        @Schema(example = "123456789") Long id,
        @Schema(example = "Bill") String firstName,
        @Schema(example = "Gates") String lastName,
        @Schema(description = "Total assets value expressed in USD", example = "17000000000.00")
        BigDecimal assetsValue,
        @Schema(description = "Whether the person qualifies as an oligarch", example = "true")
        boolean oligarch) {
}
