package com.mycompany.currency.converter.Infrastructure.exception;

public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String message) {
        super(message);
    }
}
