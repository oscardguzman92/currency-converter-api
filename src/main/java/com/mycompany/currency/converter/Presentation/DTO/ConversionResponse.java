package com.mycompany.currency.converter.Presentation.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ConversionResponse {
    private LocalDate conversionDate;
    private Double exchangeRate;
    private Double originalAmount;
    private Double convertedAmount;
    private String sourceCurrency;
    private String targetCurrency;
}
