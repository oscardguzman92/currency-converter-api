package com.mycompany.currency.converter.Presentation.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ConversionResponse {
    private LocalDate conversionDate;
    private BigDecimal exchangeRate;
    private BigDecimal originalAmount;
    private BigDecimal convertedAmount;
    private String sourceCurrency;
    private String targetCurrency;
}
