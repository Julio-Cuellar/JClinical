package com.jcode.patientservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.patients}")
    private String patientsExchange;

    @Value("${app.rabbitmq.queue.patient-created}")
    private String patientCreatedQueue;

    @Value("${app.rabbitmq.queue.patient-updated}")
    private String patientUpdatedQueue;

    @Value("${app.rabbitmq.queue.patient-deactivated}")
    private String patientDeactivatedQueue;

    @Value("${app.rabbitmq.routing-key.patient-created}")
    private String patientCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.patient-updated}")
    private String patientUpdatedRoutingKey;

    @Value("${app.rabbitmq.routing-key.patient-deactivated}")
    private String patientDeactivatedRoutingKey;

    @Bean
    public TopicExchange patientsExchange() {
        return new TopicExchange(patientsExchange);
    }

    @Bean
    public Queue patientCreatedQueue() {
        return QueueBuilder.durable(patientCreatedQueue).build();
    }

    @Bean
    public Queue patientUpdatedQueue() {
        return QueueBuilder.durable(patientUpdatedQueue).build();
    }

    @Bean
    public Queue patientDeactivatedQueue() {
        return QueueBuilder.durable(patientDeactivatedQueue).build();
    }

    @Bean
    public Binding patientCreatedBinding() {
        return BindingBuilder.bind(patientCreatedQueue())
                .to(patientsExchange())
                .with(patientCreatedRoutingKey);
    }

    @Bean
    public Binding patientUpdatedBinding() {
        return BindingBuilder.bind(patientUpdatedQueue())
                .to(patientsExchange())
                .with(patientUpdatedRoutingKey);
    }

    @Bean
    public Binding patientDeactivatedBinding() {
        return BindingBuilder.bind(patientDeactivatedQueue())
                .to(patientsExchange())
                .with(patientDeactivatedRoutingKey);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        // Usará el converter por defecto (SimpleMessageConverter),
        // y nosotros enviaremos el payload ya serializado a JSON (String).
        return new RabbitTemplate(connectionFactory);
    }
}
