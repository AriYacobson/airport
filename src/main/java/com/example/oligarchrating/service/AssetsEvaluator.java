package com.example.oligarchrating.service;

import com.example.oligarchrating.api.dto.FinancialAssets;
import com.example.oligarchrating.client.AssetsValuationClient;
import com.example.oligarchrating.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class AssetsEvaluator {

    private static final int USD_SCALE = 2;

    private final AssetsValuationClient assetsValuationClient;
    private final Executor upstreamExecutor;

    public AssetsEvaluator(AssetsValuationClient assetsValuationClient,
                           @Qualifier("upstreamExecutor") Executor upstreamExecutor) {
        this.assetsValuationClient = assetsValuationClient;
        this.upstreamExecutor = upstreamExecutor;
    }

    /**
     * Evaluates the total financial assets value in USD as
     * {@code cashInUsd + bitcoinAmount * bitcoinValueInUsd}.
     * The two upstream calls are issued in parallel.
     */
    public BigDecimal evaluateInUsd(FinancialAssets assets) {
        CompletableFuture<BigDecimal> cashFuture = CompletableFuture.supplyAsync(
                () -> assetsValuationClient.evaluateCashInUsd(assets.cashAmount(), assets.currency()),
                upstreamExecutor);
        CompletableFuture<BigDecimal> bitcoinFuture = CompletableFuture.supplyAsync(
                assetsValuationClient::getBitcoinValueInUsd,
                upstreamExecutor);

        BigDecimal cashInUsd = await(cashFuture);
        BigDecimal bitcoinValueInUsd = await(bitcoinFuture);
        BigDecimal total = cashInUsd
                .add(assets.bitcoinAmount().multiply(bitcoinValueInUsd))
                .setScale(USD_SCALE, RoundingMode.HALF_UP);

        log.debug("Evaluated assets total={}", total);
        return total;
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new ExternalServiceException(
                    AssetsValuationClient.SERVICE_NAME, "assets evaluation failed", cause);
        }
    }
}
