package com.mycompany.currency.converter.Application.service;

import com.mycompany.currency.converter.Application.ports.out.ExchangeRateProviderStrategy;
import com.mycompany.currency.converter.Infrastructure.exception.CurrencyNotFoundException;
import com.mycompany.currency.converter.Presentation.DTO.ConversionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class CurrencyConversionService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final String BASE_CURRENCY_FOR_API = "EUR";

    private final ExchangeRateProviderStrategy exchangeRateProvider;

    public CurrencyConversionService(ExchangeRateProviderStrategy exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }

    public ConversionResponse convertCurrency(Double amount, String sourceCurrency, String targetCurrency) throws CurrencyNotFoundException {
        logger.info("Starting currency conversion: {} {} to {}", amount, sourceCurrency, targetCurrency);

        // 1. Get rates from the external API (relative to EUR)
        Map<String, Double> ratesInEuro = exchangeRateProvider.getRatesInEuro();

        // 2. Validate existence of source and target currencies in the fetched rates
        if (!ratesInEuro.containsKey(sourceCurrency)) {
            logger.error("Source currency not found in exchange rates: {}", sourceCurrency);
            throw new CurrencyNotFoundException("Source currency not supported: " + sourceCurrency);
        }
        if (!ratesInEuro.containsKey(targetCurrency)) {
            logger.error("Target currency not found in exchange rates: {}", targetCurrency);
            throw new CurrencyNotFoundException("Target currency not supported: " + targetCurrency);
        }

        // Ensure EUR is also present in rates as 1.0 (for consistency in calculations)
        ratesInEuro.putIfAbsent(BASE_CURRENCY_FOR_API, 1.0);

        // 3. Perform the conversion: Source -> EUR -> Target
        // Get rate for 1 EUR = X SourceCurrency
        Double rateSourceToEuro = ratesInEuro.get(sourceCurrency);
        // Get rate for 1 EUR = X TargetCurrency
        Double rateTargetToEuro = ratesInEuro.get(targetCurrency);

        if (rateSourceToEuro == null || rateSourceToEuro == 0.0) {
            logger.error("Invalid or zero rate for source currency: {}", sourceCurrency);
            throw new CurrencyNotFoundException("Invalid exchange rate for source currency: " + sourceCurrency);
        }
        if (rateTargetToEuro == null || rateTargetToEuro == 0.0) {
            logger.error("Invalid or zero rate for target currency: {}", targetCurrency);
            throw new CurrencyNotFoundException("Invalid exchange rate for target currency: " + targetCurrency);
        }

        // Convert original amount from SourceCurrency to EUR
        // If 1 EUR = X SourceCurrency, then Y SourceCurrency = Y/X EUR
        Double amountInEuro = amount / rateSourceToEuro;

        // Convert amount from EUR to TargetCurrency
        // If 1 EUR = Z TargetCurrency, then amountInEuro EUR = amountInEuro * Z TargetCurrency
        Double convertedAmount = amountInEuro * rateTargetToEuro;

        // 4. Build and return the response using the Builder Pattern
        // The exchange rate presented to the user should be Direct Rate (Source to Target)
        Double directExchangeRate = rateTargetToEuro / rateSourceToEuro;

        logger.info("Conversion successful: {} {} to {} {} with rate {}", amount, sourceCurrency, convertedAmount, targetCurrency, directExchangeRate);
        return ConversionResponse.builder()
                .conversionDate(LocalDate.now())
                .exchangeRate(directExchangeRate)
                .originalAmount(amount)
                .convertedAmount(convertedAmount)
                .sourceCurrency(sourceCurrency)
                .targetCurrency(targetCurrency)
                .build();
    }

}
