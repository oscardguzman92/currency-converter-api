package com.mycompany.currency.converter.Application.ports.out;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeRateProviderStrategy {
    Map<String, BigDecimal> getRatesInEuro();
}
