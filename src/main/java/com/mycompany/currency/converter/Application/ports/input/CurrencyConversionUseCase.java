package com.mycompany.currency.converter.Application.ports.input;

import com.mycompany.currency.converter.Presentation.DTO.ConversionResponse;

import java.math.BigDecimal;

public interface CurrencyConversionUseCase {
    ConversionResponse convertCurrency(BigDecimal amount, String sourceCurrency, String targetCurrency);
}
