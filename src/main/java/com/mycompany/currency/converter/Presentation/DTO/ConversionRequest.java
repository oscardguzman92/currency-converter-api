package com.mycompany.currency.converter.Presentation.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ConversionRequest {
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private Double amount;

    @NotBlank(message = "Original currency cannot be blank")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Original currency must be 3-letter ISO code")
    private String sourceCurrency;

    @NotBlank(message = "Target currency cannot be blank")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Target currency must be a 3-letter ISO code")
    private String targetCurrency;
}
