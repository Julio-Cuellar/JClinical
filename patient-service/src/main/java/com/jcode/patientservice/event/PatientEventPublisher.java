package com.jcode.patientservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.exchange.patients}")
    private String patientsExchange;

    @Value("${app.rabbitmq.routing-key.patient-created}")
    private String patientCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.patient-updated}")
    private String patientUpdatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.patient-deactivated}")
    private String patientDeactivatedRoutingKey;

    public void publishPatientCreated(PatientCreatedEvent event) {
        try {
            log.info("Publicando evento PATIENT_CREATED para paciente: {} en tenantCode: {}",
                    event.getPatientId(), event.getTenantCode());

            String payload = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    patientsExchange,
                    patientCreatedRoutingKey,
                    payload,
                    message -> {
                        message.getMessageProperties().getHeaders().putAll(Map.of(
                                "eventType", event.getEventType(),
                                "tenantCode", event.getTenantCode()
                        ));
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    }
            );

            log.debug("Evento PATIENT_CREATED publicado exitosamente");
        } catch (Exception e) {
            log.error("Error publicando evento PATIENT_CREATED: {}", e.getMessage(), e);
        }
    }

    public void publishPatientUpdated(PatientUpdatedEvent event) {
        try {
            log.info("Publicando evento PATIENT_UPDATED para paciente: {} en tenantCode: {}",
                    event.getPatientId(), event.getTenantCode());

            String payload = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    patientsExchange,
                    patientUpdatedRoutingKey,
                    payload,
                    message -> {
                        message.getMessageProperties().getHeaders().putAll(Map.of(
                                "eventType", event.getEventType(),
                                "tenantCode", event.getTenantCode()
                        ));
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    }
            );

            log.debug("Evento PATIENT_UPDATED publicado exitosamente");
        } catch (Exception e) {
            log.error("Error publicando evento PATIENT_UPDATED: {}", e.getMessage(), e);
        }
    }

    public void publishPatientDeactivated(PatientDeactivatedEvent event) {
        try {
            log.info("Publicando evento PATIENT_DEACTIVATED para paciente: {} en tenantCode: {}",
                    event.getPatientId(), event.getTenantCode());

            String payload = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    patientsExchange,
                    patientDeactivatedRoutingKey,
                    payload,
                    message -> {
                        message.getMessageProperties().getHeaders().putAll(Map.of(
                                "eventType", event.getEventType(),
                                "tenantCode", event.getTenantCode()
                        ));
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    }
            );

            log.debug("Evento PATIENT_DEACTIVATED publicado exitosamente");
        } catch (Exception e) {
            log.error("Error publicando evento PATIENT_DEACTIVATED: {}", e.getMessage(), e);
        }
    }
}
