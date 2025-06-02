package com.mycompany.currency.converter.Application.ports.out;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeCacheProvider {
    void saveRates(Map<String, BigDecimal> rates);
    Map<String, BigDecimal> getRates();
    boolean hasRates();
}
