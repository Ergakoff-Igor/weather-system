package com.weather.gateway.service;

import com.weather.shared.dto.WeatherDataDto;
import com.weather.shared.dto.WeatherForecastDto;
import com.weather.shared.messaging.WeatherMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static com.weather.shared.config.RabbitMQConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    @Value("${weather.processing.service.url:http://localhost:8081}")
    private String processingServiceUrl;

    public void processWeatherData(WeatherDataDto weatherData) {
        WeatherMessage message = new WeatherMessage(
                weatherData.getStationId(),
                weatherData.getTimestamp(),
                weatherData.getTemperature(),
                weatherData.getHumidity(),
                weatherData.getPressure(),
                weatherData.getPrecipitation()
        );

        try {
            rabbitTemplate.convertAndSend(WEATHER_DATA_EXCHANGE, WEATHER_DATA_ROUTING_KEY, message);
            log.debug("Weather data sent to RabbitMQ for station: {}", weatherData.getStationId());
        } catch (Exception e) {
            log.error("Failed to send weather data to RabbitMQ for station: {}", weatherData.getStationId(), e);
            throw new RuntimeException("Failed to process weather data", e);
        }
    }

    public WeatherForecastDto getWeatherForecast(String stationId, int hours) {
        String url = String.format("%s/api/v1/weather/forecast?stationId=%s&hours=%d",
                processingServiceUrl, stationId, hours);

        try {
            WeatherForecastDto forecast = restTemplate.getForObject(url, WeatherForecastDto.class);
            log.debug("Retrieved forecast for station: {}, hours: {}", stationId, hours);
            return forecast;
        } catch (Exception e) {
            log.error("Failed to get forecast for station: {}", stationId, e);
            throw new RuntimeException("Failed to get weather forecast", e);
        }
    }
}