package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.FinancialAssets;
import com.example.oligarchrating.client.AssetsValuationClient;
import com.example.oligarchrating.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetsEvaluatorTest {

    @Mock
    AssetsValuationClient assetsValuationClient;

    AssetsEvaluator evaluator;

    @BeforeEach
    void setUp() {
        Executor sameThread = Runnable::run;
        evaluator = new AssetsEvaluator(assetsValuationClient, sameThread);
    }

    @Test
    void summarizesCashAndBitcoinInUsd() {
        FinancialAssets assets = new FinancialAssets(
                new BigDecimal("16000000000"),
                Currency.getInstance("ILS"),
                new BigDecimal("50"));
        when(assetsValuationClient.evaluateCashInUsd(assets.cashAmount(), assets.currency()))
                .thenReturn(new BigDecimal("4000000000"));
        when(assetsValuationClient.getBitcoinValueInUsd()).thenReturn(new BigDecimal("60000"));

        BigDecimal total = evaluator.evaluateInUsd(assets);

        assertThat(total).isEqualByComparingTo("4003000000.00");
    }

    @Test
    void zeroBitcoinAmountYieldsCashOnly() {
        FinancialAssets assets = new FinancialAssets(
                new BigDecimal("100"),
                Currency.getInstance("USD"),
                BigDecimal.ZERO);
        when(assetsValuationClient.evaluateCashInUsd(assets.cashAmount(), assets.currency()))
                .thenReturn(new BigDecimal("100"));
        when(assetsValuationClient.getBitcoinValueInUsd()).thenReturn(new BigDecimal("60000"));

        BigDecimal total = evaluator.evaluateInUsd(assets);

        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void propagatesExternalServiceExceptionFromUpstream() {
        FinancialAssets assets = new FinancialAssets(
                new BigDecimal("100"),
                Currency.getInstance("USD"),
                BigDecimal.ZERO);
        when(assetsValuationClient.evaluateCashInUsd(assets.cashAmount(), assets.currency()))
                .thenThrow(new ExternalServiceException("assets-valuation", "boom"));

        assertThatThrownBy(() -> evaluator.evaluateInUsd(assets))
                .isInstanceOf(ExternalServiceException.class);
    }
}
