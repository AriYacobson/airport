package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.FinancialAssets;
import com.example.oligarchrating.client.AssetsValuationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetsEvaluator {

    private static final int USD_SCALE = 2;

    private final AssetsValuationClient assetsValuationClient;

    /**
     * Evaluates the total financial assets value in USD as:
     * {@code cashInUsd + bitcoinAmount * bitcoinValueInUsd}
     */
    public BigDecimal evaluateInUsd(FinancialAssets assets) {
        BigDecimal cashInUsd = assetsValuationClient.evaluateCashInUsd(
                assets.cashAmount(), assets.currency());
        BigDecimal bitcoinValueInUsd = assetsValuationClient.getBitcoinValueInUsd();
        BigDecimal bitcoinTotal = assets.bitcoinAmount().multiply(bitcoinValueInUsd);
        BigDecimal total = cashInUsd.add(bitcoinTotal).setScale(USD_SCALE, RoundingMode.HALF_UP);
        log.debug("Evaluated assets: cashUsd={} btcUsd={} btcAmount={} total={}",
                cashInUsd, bitcoinValueInUsd, assets.bitcoinAmount(), total);
        return total;
    }
}
