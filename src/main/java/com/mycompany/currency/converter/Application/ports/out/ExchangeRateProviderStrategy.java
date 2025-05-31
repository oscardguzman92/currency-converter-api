package com.mycompany.currency.converter.Application.ports.out;

import java.util.Map;

public interface ExchangeRateProviderStrategy {
    Map<String, Double> getRatesInEuro();
}
