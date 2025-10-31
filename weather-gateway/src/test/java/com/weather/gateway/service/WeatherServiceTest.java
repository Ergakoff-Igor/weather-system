package com.weather.gateway.service;

import com.weather.shared.dto.WeatherDataDto;
import com.weather.shared.dto.WeatherForecastDto;
import com.weather.shared.messaging.WeatherMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static com.weather.shared.config.RabbitMQConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<WeatherMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<String> exchangeCaptor;

    @Captor
    private ArgumentCaptor<String> routingKeyCaptor;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(rabbitTemplate, restTemplate);
        // Используем ReflectionTestUtils для установки приватного поля
        ReflectionTestUtils.setField(weatherService, "processingServiceUrl", "http://localhost:8081");
    }

    @Test
    void shouldProcessWeatherDataSuccessfully() {
        // Given
        WeatherDataDto weatherData = new WeatherDataDto(
                "station-1",
                LocalDateTime.now(),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        // When
        weatherService.processWeatherData(weatherData);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(WEATHER_DATA_EXCHANGE),  // Явно указываем exchange
                eq(WEATHER_DATA_ROUTING_KEY), // Явно указываем routing key
                any(WeatherMessage.class)   // Любой объект сообщения
        );

        // Альтернативно можно использовать ArgumentCaptor с явными типами
        verify(rabbitTemplate).convertAndSend(
                eq(WEATHER_DATA_EXCHANGE),
                eq(WEATHER_DATA_ROUTING_KEY),
                any(WeatherMessage.class)
        );
    }

    @Test
    void shouldGetWeatherForecastSuccessfully() {
        // Given
        String stationId = "station-1";
        int hours = 3;
        WeatherForecastDto expectedForecast = createTestForecast();

        when(restTemplate.getForObject(anyString(), eq(WeatherForecastDto.class)))
                .thenReturn(expectedForecast);

        // When
        WeatherForecastDto result = weatherService.getWeatherForecast(stationId, hours);

        // Then
        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        assertEquals(1, result.getForecasts().size());

        verify(restTemplate).getForObject(
                "http://localhost:8081/api/v1/weather/forecast?stationId=station-1&hours=3",
                WeatherForecastDto.class
        );
    }

    @Test
    void shouldThrowExceptionWhenRabbitMQFails() {
        // Given
        WeatherDataDto weatherData = new WeatherDataDto(
                "station-1",
                LocalDateTime.now(),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        // Используем явные параметры чтобы избежать неоднозначности
        doThrow(new RuntimeException("RabbitMQ error"))
                .when(rabbitTemplate).convertAndSend(eq(WEATHER_DATA_EXCHANGE), eq(WEATHER_DATA_ROUTING_KEY), any(WeatherMessage.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> weatherService.processWeatherData(weatherData));

        assertEquals("Failed to process weather data", exception.getMessage());
    }

    private WeatherForecastDto createTestForecast() {
        WeatherForecastDto.ForecastItem forecastItem = new WeatherForecastDto.ForecastItem(
                LocalDateTime.now().plusHours(1),
                26.0,
                63.0,
                1013.5,
                0.0
        );

        return new WeatherForecastDto(
                "station-1",
                LocalDateTime.now(),
                java.util.List.of(forecastItem)
        );
    }
}