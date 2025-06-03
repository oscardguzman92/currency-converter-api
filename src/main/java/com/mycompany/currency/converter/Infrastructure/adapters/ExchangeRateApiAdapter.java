package com.mycompany.currency.converter.Infrastructure.adapters;

import com.mycompany.currency.converter.Application.ports.out.ExchangeRateProviderStrategy;
import com.mycompany.currency.converter.Application.ports.out.NotificationService;
import com.mycompany.currency.converter.Infrastructure.exception.ExternalApiException;
import com.mycompany.currency.converter.Presentation.DTO.ExchangeRateApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExchangeRateApiAdapter implements ExchangeRateProviderStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateApiAdapter.class);
    private final RestTemplate restTemplate;
    private final InMemoryExchangeRateCacheAdapter cacheProvider;
    private final NotificationService notificationService;

    @Value("${external.api.currency.url}")
    private String externalApiUrl;

    private static final String EXPECTED_BASE_CURRENCY = "EUR";

    public ExchangeRateApiAdapter(RestTemplate restTemplate, InMemoryExchangeRateCacheAdapter cacheProvider, NotificationService notificationService) {
        this.restTemplate = restTemplate;
        this.cacheProvider = cacheProvider;
        this.notificationService = notificationService;
    }

    @Override
    public Map<String, BigDecimal> getRatesInEuro() {
        try {
            logger.info("Fetching exchange rates from external API: {}", externalApiUrl);
            ExchangeRateApiResponse response = restTemplate.getForObject(externalApiUrl, ExchangeRateApiResponse.class);

            if(response == null) {
                notificationService.notifyExternalServiceFailure("ExchangeRateAPI", "API returned null response from " + externalApiUrl, null);
                return handleFallback("External API returned null response", null);
            }

            //Validar la moneda base
            if(!EXPECTED_BASE_CURRENCY.equals(response.getBase())) {
                notificationService.notifyExternalServiceFailure("ExchangeRateAPI", "API returned unexpected base currency. Expected: EUR, Got: " + response.getBase() + " from " + externalApiUrl, null);
                return handleFallback("External API returned rates with an unexpected base currency. Expected EUR.", null);
            }

            if(!response.isSuccess()) {
                notificationService.notifyExternalServiceFailure("ExchangeRateAPI", "API indicated failure for " + externalApiUrl + ". Response: " + response, null);
                return handleFallback("External API indicated an error.", null);
            }

            if (response.getRates() != null && !response.getRates().isEmpty()) {
                logger.debug("Successfully fetched rates. Date: {}, Rates count: {}", response.getDate(), response.getRates().size());

                Map<String, BigDecimal> bigDecimalRates = response.getRates().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> BigDecimal.valueOf(entry.getValue()) // Convert Double to BigDecimal
                        ));
                cacheProvider.saveRates(bigDecimalRates);
                return bigDecimalRates;
            } else {
                notificationService.notifyExternalServiceFailure("ExchangeRateAPI", "API returned null or empty rates from " + externalApiUrl, null);
                return handleFallback("External API returned null or empty rates.", null);
            }
        } catch (org.springframework.web.client.RestClientException e) {
            notificationService.notifyExternalServiceFailure("ExchangeRateAPI", "Network or HTTP error while fetching rates from " + externalApiUrl, e);
             return handleFallback("Failed to connect to external exchange rate service or received an HTTP error.", e);

        } catch (Exception e) {
            notificationService.notifyExternalServiceFailure("ExchangeRateAPI", "An unexpected error occurred while processing API response from " + externalApiUrl, e);
            return handleFallback("An unexpected error occurred while processing external API response.", e);
        }
    }

    private Map<String, BigDecimal> handleFallback(String specificErrorMessage, Throwable originalCause) {
        logger.warn("Applying fallback for ExchangeRateApiAdapter due to: {}. Attempting to retrieve rates from cache.", specificErrorMessage);
        Map<String, BigDecimal> cachedRates = cacheProvider.getRates(); // Use getRates() as defined in your cache adapter
        if (cachedRates != null && !cachedRates.isEmpty()) {
            logger.info("Successfully retrieved {} rates from cache as fallback.", cachedRates.size());
            return cachedRates;
        } else {
            logger.error("Cache is empty. Cannot provide fallback rates. Throwing ExternalApiException.");
            throw new ExternalApiException("External exchange rate service is unavailable and no cached rates are available. " + specificErrorMessage, originalCause);
        }
    }
}
