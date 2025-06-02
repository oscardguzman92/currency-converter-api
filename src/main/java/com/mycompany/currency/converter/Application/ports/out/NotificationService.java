package com.mycompany.currency.converter.Application.ports.out;

public interface NotificationService {
    void notifyExternalServiceFailure(String serviceName, String errorMessage, Throwable cause);
}
