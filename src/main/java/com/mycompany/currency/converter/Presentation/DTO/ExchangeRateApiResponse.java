package com.mycompany.currency.converter.Presentation.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class ExchangeRateApiResponse {
    private boolean success;
    private Long timestamp;
    private String base;
    private LocalDate date;

    @JsonProperty("rates")
    private Map<String, Double> rates;
}
