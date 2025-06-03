package com.mycompany.currency.converter.Application.service;

import com.mycompany.currency.converter.Application.ports.input.CurrencyConversionUseCase;
import com.mycompany.currency.converter.Application.ports.out.ExchangeRateProviderStrategy;
import com.mycompany.currency.converter.Infrastructure.exception.CurrencyNotFoundException;
import com.mycompany.currency.converter.Presentation.DTO.ConversionResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;


@Service
public class CurrencyConversionService implements CurrencyConversionUseCase {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final String BASE_CURRENCY_FOR_API = "EUR";

    //Escala de precisión para cálculos monetarios
    private static final int CURRENCY_SCALE = 2; // Para montos finales
    private static final int RATE_SCALE = 8;     // Más precisión para tasas de cambio y cálculos intermedios
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP; // Redondeo estándar

    private final ExchangeRateProviderStrategy exchangeRateProvider;
    private final Counter successfulConversionsCounter;
    private final Counter failedConversionsCounter;
    private final Timer conversionDurationTimer;

    public CurrencyConversionService(ExchangeRateProviderStrategy exchangeRateProvider, MeterRegistry meterRegistry) {
        this.exchangeRateProvider = exchangeRateProvider;
        // Initialize counters and timers
        this.successfulConversionsCounter = Counter.builder("currency.conversions.success.total")
                .description("Total number of successful currency conversions")
                .register(meterRegistry);
        this.failedConversionsCounter = Counter.builder("currency.conversions.failed.total")
                .description("Total number of failed currency conversions")
                .register(meterRegistry);
        this.conversionDurationTimer = Timer.builder("currency.conversions.duration.seconds")
                .description("Duration of currency conversion operations")
                .register(meterRegistry);
    }

    @Override
    public ConversionResponse convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency) throws CurrencyNotFoundException {
    long startTime = System.nanoTime();
        try {
            logger.info("Starting currency conversion: {} {} to {}", amount, sourceCurrency, targetCurrency);

            // 1. Get rates from the external API (relative to EUR)
            Map<String, BigDecimal> ratesInEuro = exchangeRateProvider.getRatesInEuro();

            // 2. Validate existence of source and target currencies in the fetched rates
            if (!ratesInEuro.containsKey(sourceCurrency)) {
                logger.error("Source currency not found in exchange rates: {}", sourceCurrency);
                throw new CurrencyNotFoundException("Source currency not supported: " + sourceCurrency);
            }
            if (!ratesInEuro.containsKey(targetCurrency)) {
                logger.error("Target currency not found in exchange rates: {}", targetCurrency);
                throw new CurrencyNotFoundException("Target currency not supported: " + targetCurrency);
            }

            // Asegurar que la moneda base EUR está presente en los rates 1.0 (para consistencia en los cálculos)
            ratesInEuro.putIfAbsent(BASE_CURRENCY_FOR_API, BigDecimal.ONE);

            // 3. Perform the conversion: Source -> EUR -> Target
            // Get rate for 1 EUR = X SourceCurrency
            BigDecimal rateSourceToEuro;
            if (sourceCurrency.equalsIgnoreCase(BASE_CURRENCY_FOR_API)) {
                rateSourceToEuro = BigDecimal.ONE;
            } else if (ratesInEuro.containsKey(sourceCurrency)) {
                rateSourceToEuro = ratesInEuro.get(sourceCurrency);
            } else {
                throw new CurrencyNotFoundException("Source currency not supported: " + sourceCurrency);
            }
            // Get rate for 1 EUR = X TargetCurrency
            BigDecimal rateTargetToEuro;
            if (targetCurrency.equalsIgnoreCase(BASE_CURRENCY_FOR_API)) {
                rateTargetToEuro = BigDecimal.ONE;
            } else if (ratesInEuro.containsKey(targetCurrency)) {
                rateTargetToEuro = ratesInEuro.get(targetCurrency);
            } else {
                throw new CurrencyNotFoundException("Target currency not supported: " + targetCurrency);
            }

            if (rateSourceToEuro == null || rateSourceToEuro.compareTo(BigDecimal.ZERO) <= 0) {
                logger.error("Invalid or zero rate for source currency: {}", sourceCurrency);
                throw new CurrencyNotFoundException("Invalid exchange rate for source currency: " + sourceCurrency);
            }
            if (rateTargetToEuro == null || rateTargetToEuro.compareTo(BigDecimal.ZERO) <= 0) {
                logger.error("Invalid or zero rate for target currency: {}", targetCurrency);
                throw new CurrencyNotFoundException("Invalid exchange rate for target currency: " + targetCurrency);
            }

            // Convert original amount from SourceCurrency to EUR
            // If 1 EUR = X SourceCurrency, then Y SourceCurrency = Y/X EUR
            //amountInEuro = amount / rateSourceToEuro;
            BigDecimal amountInEuro = amount.divide(rateSourceToEuro, MathContext.DECIMAL64);

            // Convert amount from EUR to TargetCurrency
            // If 1 EUR = Z TargetCurrency, then amountInEuro EUR = amountInEuro * Z TargetCurrency
            //convertedAmount = amountInEuro * rateTargetToEuro;
            BigDecimal convertedAmount = amountInEuro.multiply(rateTargetToEuro, MathContext.DECIMAL64);

            // 4. Build and return the response using the Builder Pattern
            // The exchange rate presented to the user should be Direct Rate (Source to Target)
            BigDecimal directExchangeRate = rateTargetToEuro.divide(rateSourceToEuro, MathContext.DECIMAL64);

            logger.info("Conversion successful: {} {} to {} {} with rate {}", amount, sourceCurrency, convertedAmount, targetCurrency, directExchangeRate);
            successfulConversionsCounter.increment();

            return ConversionResponse.builder()
                    .conversionDate(LocalDate.now())
                    .exchangeRate(directExchangeRate)
                    .originalAmount(amount)
                    .convertedAmount(convertedAmount)
                    .sourceCurrency(sourceCurrency)
                    .targetCurrency(targetCurrency)
                    .build();
        } catch (Exception e) {
            failedConversionsCounter.increment(); // Increment failure counter
            throw e; // Re-throw the exception after counting
        } finally {
            // Record the duration regardless of success or failure
            conversionDurationTimer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }

    }

}
