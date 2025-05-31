package com.mycompany.currency.converter.Infrastructure.adapters;

import com.mycompany.currency.converter.Application.ports.out.ExchangeRateProviderStrategy;
import com.mycompany.currency.converter.Infrastructure.exception.ExternalApiException;
import com.mycompany.currency.converter.Presentation.DTO.ExchangeRateApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Component
public class ExchangeRateApiAdapter implements ExchangeRateProviderStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateApiAdapter.class);
    private final RestTemplate restTemplate;

    @Value("${external.api.currency.url}")
    private String externalApiUrl;

    private static final String EXPECTED_BASE_CURRENCY = "EUR";

    public ExchangeRateApiAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, Double> getRatesInEuro() {
        try {
            logger.info("Fetching exchange rates from external API: {}", externalApiUrl);
            ExchangeRateApiResponse response = restTemplate.getForObject(externalApiUrl, ExchangeRateApiResponse.class);

            if(response == null) {
                logger.error("External API returned null response for URL:{}", externalApiUrl);
                throw new ExternalApiException("External API returned null response");
            }

            //Validar la moneda base
            if(!EXPECTED_BASE_CURRENCY.equals(response.getBase())) {
                logger.error("External API returned unexpected base currency. Expected: {}, Got: {}", EXPECTED_BASE_CURRENCY, response.getBase());
                throw new ExternalApiException("External API returned rates with an unexpected base currency. Expected EUR.");
            }

            if(!response.isSuccess()) {
                logger.error("External API indicated failure: {}", response);
                throw new ExternalApiException("External API indicated an error.")
            }

            if (response != null && response.getRates() != null && !response.getRates().isEmpty()) {
                logger.debug("Successfully fetched rates. Date: {}, Rates count: {}", response.getDate(), response.getRates().size());
                return response.getRates();
            } else {
                logger.warn("External API returned null or empty rates. URL: {}", externalApiUrl);
                return Collections.emptyMap();
            }
        } catch (org.springframework.web.client.RestClientException e) {
            logger.error("Network or HTTP error while fetching rates from external API: {}", e.getMessage());
            throw new ExternalApiException("Failed to connect to external exchange rate service or received an HTTP error.", e);

        } catch (Exception e) {
            logger.error("Error fetching exchange rates from external API: {}",e.getMessage());
            throw new RuntimeException("An unxpected error occurred while processing external API response.", e);
        }
    }
}
