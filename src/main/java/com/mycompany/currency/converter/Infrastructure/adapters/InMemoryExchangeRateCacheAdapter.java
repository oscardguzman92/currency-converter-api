package com.mycompany.currency.converter.Infrastructure.adapters;

import com.mycompany.currency.converter.Application.ports.out.ExchangeCacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryExchangeRateCacheAdapter implements ExchangeCacheProvider {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryExchangeRateCacheAdapter.class);
    private final Map<String, BigDecimal> cachedRates = new ConcurrentHashMap<>();

    @Override
    public void saveRates(Map<String, BigDecimal> rates) {
        if (rates != null && !rates.isEmpty()) {
            this.cachedRates.clear();
            this.cachedRates.putAll(rates);
            logger.info("Exchange rates saved to cache. Total rates: {}", rates.size());
        } else {
            logger.warn("Attempted to save null or empty rates to cache.");
        }
    }

    @Override
    public Map<String, BigDecimal> getRates() {
        if (cachedRates.isEmpty()) {
            logger.warn("No exchange rates found in cache. Returning empty map.");
            return Collections.emptyMap();
        }
        logger.info("Retrieving {} exchange rates from cache.", cachedRates.size());
        return new HashMap<>(cachedRates);
    }

    @Override
    public boolean hasRates() {
        return !cachedRates.isEmpty();
    }
}



