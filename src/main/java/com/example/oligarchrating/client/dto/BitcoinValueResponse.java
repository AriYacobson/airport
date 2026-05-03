package com.example.oligarchrating.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BitcoinValueResponse(BigDecimal valueUsd) {
}
