package com.example.oligarchrating.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Currency;

@Schema(description = "Financial assets owned by the person")
public record FinancialAssets(
        @NotNull
        @PositiveOrZero
        @Schema(example = "16000000000")
        BigDecimal cashAmount,

        @NotNull
        @Schema(example = "ILS", description = "ISO 4217 currency code of the cash amount")
        Currency currency,

        @NotNull
        @PositiveOrZero
        @Schema(example = "50")
        BigDecimal bitcoinAmount) {
}
