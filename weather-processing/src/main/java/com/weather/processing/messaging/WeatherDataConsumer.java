package com.weather.processing.messaging;

import com.weather.processing.service.WeatherDataService;
import com.weather.shared.messaging.WeatherMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherDataConsumer {

    private final WeatherDataService weatherDataService;

    @RabbitListener(queues = "${weather.rabbitmq.queue:weather.data.queue}")
    public void receiveWeatherData(WeatherMessage message) {
        try {
            log.debug("Received weather data message for station: {}", message.getStationId());
            weatherDataService.saveWeatherData(message);
        } catch (Exception e) {
            log.error("Failed to process weather data message for station: {}", message.getStationId(), e);
            throw new RuntimeException("Failed to process weather data", e);
        }
    }
}