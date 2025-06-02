package com.mycompany.currency.converter.Infrastructure.adapters;

import com.mycompany.currency.converter.Application.ports.out.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class LoggingNotificationAdapter implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    //Mapa para almacenar el último timestamp de notificación por "tipo de problema"
    //Clave: serviceName + "::" + errorMessage (para problemas específicos)
    //Valor: LocalDateTime del último envío de notificación
    private final ConcurrentHashMap<String, LocalDateTime> lastNotificationTimestamps = new ConcurrentHashMap<>();

    //Cooldown de notificaciones: 5 minutos
    private static final long NOTIFICATION_COOLDOWN_MINUTES = 5;


    @Override
    public void notifyExternalServiceFailure(String serviceName, String errorMessage, Throwable cause) {
        //Clave única para este tipo específico de problema
        String problemKey = serviceName + "::" + errorMessage;

        //Obtener el último timestamp de notificaciones para esta clave
        LocalDateTime lastNotified = lastNotificationTimestamps.get(problemKey);
        LocalDateTime now = LocalDateTime.now();

        //Verificar si es la primera vez, o si ha pasado el período de enfriamiento
        if (lastNotified == null || lastNotified.plusMinutes(NOTIFICATION_COOLDOWN_MINUTES).isBefore(now)) {
            performNotification(serviceName, errorMessage, cause);
            // Actualizar el timestamp de la última notificación para esta clave
            lastNotificationTimestamps.put(problemKey, now);
        } else {
            logger.info("Notification for service '{}' with error '{}' is currently in cooldown. Skipping.", serviceName, errorMessage);
        }

        logger.error("!!! ALERT: External Service Failure Notification !!!");
        logger.error("Service: {}", serviceName);
        logger.error("Error: {}", errorMessage);
        if (cause != null) {
            logger.error("Cause: ", cause); // Log the full stack trace of the original cause
        }
        logger.error("Action: Please investigate the availability of {}.", serviceName);
    }

    @Async
    private void performNotification(String serviceName, String errorMessage, Throwable cause) {
        //Simulamos un retraso en la notificación (ej. llamada  a un API externa como Slack API)
        //Thread.sleep(2000);

        logger.error("!!! ALERT: External Service Failure Notification !!!");
        logger.error("Service: {}", serviceName);
        logger.error("Error: {}", errorMessage);
        if (cause != null) {
            logger.error("Cause: ", cause); // Log the full stack trace of the original cause
        }
        logger.error("Action: Please investigate the availability of {}.", serviceName);
        //En un entorno real de producción, podríamos integrarlo con Slack API, Email API, PagerDuty, etc.
    }
}
