package com.mycompany.currency.converter.Application.service;

import com.mycompany.currency.converter.Application.ports.out.ExchangeRateProviderStrategy;
import com.mycompany.currency.converter.Presentation.DTO.ConversionResponse;
import com.mycompany.currency.converter.Infrastructure.exception.CurrencyNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) //Mockito for JUnit 5
@DisplayName("CurrencyConversionService Unit Tests")
public class CurrencyConversionServiceTest {
    @Mock // Mocking the dependency
    private ExchangeRateProviderStrategy exchangeRateProvider;

    // Use a real MeterRegistry for testing metrics, or mock if needed for specific scenarios
    private MeterRegistry meterRegistry;

    private CurrencyConversionService conversionService;

    // Define un contexto de precisión para las aserciones de BigDecimal si es necesario
    private static final MathContext TEST_MATH_CONTEXT = new MathContext(10); // 10 decimales de precisión para tests

    @BeforeEach
    void setUp() {
        // Initialize a simple MeterRegistry for testing metrics
        meterRegistry = new SimpleMeterRegistry();
        // Manually inject meterRegistry
        conversionService = new CurrencyConversionService(exchangeRateProvider, meterRegistry);
    }

    @Test
    @DisplayName("Should successfully convert EUR to USD")
    void shouldSuccessfullyConvertEurToUsd() throws CurrencyNotFoundException {
        // Given
        BigDecimal amount = new BigDecimal("100.0");
        String sourceCurrency = "EUR";
        String targetCurrency = "USD";

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("1.0")); // 1 EUR = 1 EUR
        rates.put("USD", new BigDecimal("1.08")); // 1 EUR = 1.08 USD (as per API example)
        when(exchangeRateProvider.getRatesInEuro()).thenReturn(rates);

        // When
        ConversionResponse response = conversionService.convertCurrency(amount, sourceCurrency, targetCurrency);

        // Then
        assertNotNull(response);
        assertEquals(LocalDate.now(), response.getConversionDate());
        assertEquals(amount.stripTrailingZeros(), response.getOriginalAmount().stripTrailingZeros());
        assertEquals(sourceCurrency, response.getSourceCurrency());
        assertEquals(targetCurrency, response.getTargetCurrency());

        //Expected BigDecimal values
        BigDecimal expectedExchangeRate = new BigDecimal("1.08");
        BigDecimal expectedConvertedAmount = new BigDecimal("108.0"); //100** 1.08

        // Comparar usando compareTo despues de redondear al contexto del test
        assertTrue(expectedConvertedAmount.round(TEST_MATH_CONTEXT).compareTo(response.getConvertedAmount().round(TEST_MATH_CONTEXT)) == 0,
                "Converted amount mismatch. Expected: " + expectedConvertedAmount.round(TEST_MATH_CONTEXT) + ", Was: " + response.getConvertedAmount().round(TEST_MATH_CONTEXT));
        assertTrue(expectedExchangeRate.round(TEST_MATH_CONTEXT).compareTo(response.getExchangeRate().round(TEST_MATH_CONTEXT)) == 0,
                "Exchange rate mismatch. Expected: " + expectedExchangeRate.round(TEST_MATH_CONTEXT) + ", Was: " + response.getExchangeRate().round(TEST_MATH_CONTEXT));

        verify(exchangeRateProvider, times(1)).getRatesInEuro();
        assertEquals(1.0, meterRegistry.counter("currency.conversions.success.total").count());
        assertEquals(0.0, meterRegistry.counter("currency.conversions.failed.total").count());
        assertTrue(meterRegistry.timer("currency.conversions.duration.seconds").count() > 0);
    }

    @Test
    @DisplayName("Should successfully convert USD to GBP")
    void shouldSuccessfullyConvertUsdToGbp() throws CurrencyNotFoundException {
        // Given
        BigDecimal amount = new BigDecimal("50.0");
        String sourceCurrency = "USD";
        String targetCurrency = "GBP";

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("1.0"));
        rates.put("USD", new BigDecimal("1.08")); // 1 EUR = 1.08 USD => 1 USD = 1/1.08 EUR
        rates.put("GBP", new BigDecimal("0.85")); // 1 EUR = 0.85 GBP => 1 GBP = 1/0.85 EUR
        when(exchangeRateProvider.getRatesInEuro()).thenReturn(rates);

        // When
        ConversionResponse response = conversionService.convertCurrency(amount, sourceCurrency, targetCurrency);

        // Then
        assertNotNull(response);
        assertEquals(amount.stripTrailingZeros(), response.getOriginalAmount().stripTrailingZeros());
        assertEquals(sourceCurrency, response.getSourceCurrency());
        assertEquals(targetCurrency, response.getTargetCurrency());

        // Calculate expected conversion:
        // 50 USD / 1.08 (USD/EUR) = 46.296 EUR
        // 46.296 EUR * 0.85 (GBP/EUR) = 39.351 GBP
        // Direct rate: (GBP/EUR) / (USD/EUR) = 0.85 / 1.08 = 0.787
        BigDecimal expectedConvertedAmount = new BigDecimal("39.35185185185", TEST_MATH_CONTEXT); // More precision for division
        BigDecimal expectedExchangeRate = new BigDecimal("0.787037037037", TEST_MATH_CONTEXT); // More precision for division

        // Comparar con un delta apropiado o redondeando ambos a una escala definida para la comparación
        assertTrue(expectedConvertedAmount.compareTo(response.getConvertedAmount().round(TEST_MATH_CONTEXT)) == 0,
                "Converted amount mismatch. Expected: " + expectedConvertedAmount + ", Was: " + response.getConvertedAmount());
        assertTrue(expectedExchangeRate.compareTo(response.getExchangeRate().round(TEST_MATH_CONTEXT)) == 0,
                "Exchange rate mismatch. Expected: " + expectedExchangeRate + ", Was: " + response.getExchangeRate());

        assertEquals(1.0, meterRegistry.counter("currency.conversions.success.total").count());
        assertEquals(0.0, meterRegistry.counter("currency.conversions.failed.total").count());
    }

    @Test
    @DisplayName("Should throw CurrencyNotFoundException if source currency is not supported")
    void shouldThrowCurrencyNotFoundExceptionForUnsupportedSource() {
        // Given
        BigDecimal amount = new BigDecimal("10.0");
        String sourceCurrency = "XYZ"; // Unsupported
        String targetCurrency = "USD";

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", BigDecimal.valueOf(1.0));
        rates.put("USD", BigDecimal.valueOf(1.08));
        when(exchangeRateProvider.getRatesInEuro()).thenReturn(rates);

        // When / Then
        CurrencyNotFoundException thrown = assertThrows(CurrencyNotFoundException.class, () -> {
            conversionService.convertCurrency(amount, sourceCurrency, targetCurrency);
        });

        assertEquals("Source currency not supported: XYZ", thrown.getMessage());
        assertEquals(0.0, meterRegistry.counter("currency.conversions.success.total").count());
        assertEquals(1.0, meterRegistry.counter("currency.conversions.failed.total").count());
    }

    @Test
    @DisplayName("Should throw CurrencyNotFoundException if target currency is not supported")
    void shouldThrowCurrencyNotFoundExceptionForUnsupportedTarget() {
        // Given
        BigDecimal amount = new BigDecimal("10.0");
        String sourceCurrency = "USD";
        String targetCurrency = "XYZ"; // Unsupported

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("1.0"));
        rates.put("USD", new BigDecimal("1.08"));
        when(exchangeRateProvider.getRatesInEuro()).thenReturn(rates);

        // When / Then
        CurrencyNotFoundException thrown = assertThrows(CurrencyNotFoundException.class, () -> {
            conversionService.convertCurrency(amount, sourceCurrency, targetCurrency);
        });

        assertEquals("Target currency not supported: XYZ", thrown.getMessage());
        assertEquals(0.0, meterRegistry.counter("currency.conversions.success.total").count());
        assertEquals(1.0, meterRegistry.counter("currency.conversions.failed.total").count());
    }

    @Test
    @DisplayName("Should handle empty rates from provider")
    void shouldHandleEmptyRates() {
        // Given
        BigDecimal amount = new BigDecimal("10.0");
        String sourceCurrency = "";
        String targetCurrency = "EUR";

        when(exchangeRateProvider.getRatesInEuro()).thenReturn(new HashMap<>()); // Empty map

        // When / Then
        CurrencyNotFoundException thrown = assertThrows(CurrencyNotFoundException.class, () -> {
            conversionService.convertCurrency(amount, sourceCurrency, targetCurrency);
        });

        // The exception will be "Source currency not supported" because EUR is putIfAbsent after check
         assertTrue(thrown.getMessage().contains("currency not supported"));
        assertEquals(0.0, meterRegistry.counter("currency.conversions.success.total").count());
        assertEquals(1.0, meterRegistry.counter("currency.conversions.failed.total").count());
    }
}
