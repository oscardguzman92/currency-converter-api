package com.mycompany.currency.converter.Presentation.controllers;

import com.mycompany.currency.converter.Application.service.CurrencyConversionService;
import com.mycompany.currency.converter.Infrastructure.exception.CurrencyNotFoundException;
import com.mycompany.currency.converter.Presentation.DTO.ConversionRequest;
import com.mycompany.currency.converter.Presentation.DTO.ConversionResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/convert")
public class CurrencyConversionController {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyConversionController.class);
    private final CurrencyConversionService conversionService;

    public CurrencyConversionController(CurrencyConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @PostMapping
    public ResponseEntity<ConversionResponse> convert(@Valid @RequestBody ConversionRequest request) throws CurrencyNotFoundException {
        logger.info("Received conversion request: {}", request);
        ConversionResponse response = conversionService.convertCurrency(
                request.getAmount(),
                request.getSourceCurrency(),
                request.getTargetCurrency()
        );
        return ResponseEntity.ok(response);
    }
}
