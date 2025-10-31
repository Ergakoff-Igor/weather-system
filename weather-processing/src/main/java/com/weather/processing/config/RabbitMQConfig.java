package com.weather.processing.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RabbitMQConfig {

    @Value("${weather.rabbitmq.queue:weather.data.queue}")
    private String queueName;

    @Value("${weather.rabbitmq.exchange:weather.data.exchange}")
    private String exchangeName;

    @Value("${weather.rabbitmq.routing-key:weather.data}")
    private String routingKey;

    @Bean
    public Queue weatherDataQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchangeName + ".dlx")
                .build();
    }

    @Bean
    public DirectExchange weatherDataExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding weatherDataBinding(Queue weatherDataQueue, DirectExchange weatherDataExchange) {
        return BindingBuilder.bind(weatherDataQueue)
                .to(weatherDataExchange)
                .with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // РЕГИСТРИРУЕМ МОДУЛЬ ДЛЯ РАБОТЫ С Instant
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter); // Используем уже созданный бин
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}