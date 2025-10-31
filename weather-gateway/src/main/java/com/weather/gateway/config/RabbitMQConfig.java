package com.weather.gateway.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static com.weather.shared.config.RabbitMQConfig.*;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue weatherDataQueue() {
        return QueueBuilder.durable(WEATHER_DATA_QUEUE)
                .withArgument("x-dead-letter-exchange", WEATHER_DATA_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public DirectExchange weatherDataExchange() {
        return new DirectExchange(WEATHER_DATA_EXCHANGE);
    }

    @Bean
    public Binding weatherDataBinding(Queue weatherDataQueue, DirectExchange weatherDataExchange) {
        return BindingBuilder.bind(weatherDataQueue)
                .to(weatherDataExchange)
                .with(WEATHER_DATA_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Регистрируем модуль для Java 8 Date/Time API (поддерживает Instant)
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}