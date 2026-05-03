package com.example.oligarchrating.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Identifying information about the person being rated")
public record PersonInformation(
        @NotBlank
        @Size(max = 100)
        @Schema(example = "Bill")
        String firstName,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "Gates")
        String lastName) {
}
