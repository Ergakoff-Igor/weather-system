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

import java.time.Instant;
import java.util.List;

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

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(rabbitTemplate, restTemplate);
        ReflectionTestUtils.setField(weatherService, "processingServiceUrl", "http://localhost:8081");
    }

    @Test
    void shouldProcessWeatherDataSuccessfully() {
        // Given
        Instant timestamp = Instant.now();
        WeatherDataDto weatherData = new WeatherDataDto(
                "station-1",
                timestamp,
                25.5,
                65.0,
                1013.25,
                0.0
        );

        // When
        weatherService.processWeatherData(weatherData);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq(WEATHER_DATA_EXCHANGE),
                eq(WEATHER_DATA_ROUTING_KEY),
                messageCaptor.capture()
        );

        WeatherMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals("station-1", capturedMessage.getStationId());
        assertEquals(timestamp, capturedMessage.getTimestamp());
        assertEquals(25.5, capturedMessage.getTemperature());
        assertEquals(65.0, capturedMessage.getHumidity());
        assertEquals(1013.25, capturedMessage.getPressure());
        assertEquals(0.0, capturedMessage.getPrecipitation());
    }

    @Test
    void shouldGetWeatherForecastSuccessfully() {
        // Given
        String stationId = "station-1";
        int hours = 3;
        WeatherForecastDto expectedForecast = createTestForecast();

        String expectedUrl = String.format("http://localhost:8081/api/v1/weather/forecast?stationId=%s&hours=%d", stationId, hours);

        when(restTemplate.getForObject(expectedUrl, WeatherForecastDto.class))
                .thenReturn(expectedForecast);

        // When
        WeatherForecastDto result = weatherService.getWeatherForecast(stationId, hours);

        // Then
        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        assertEquals(1, result.getForecasts().size());

        WeatherForecastDto.ForecastItem forecastItem = result.getForecasts().get(0);
        assertEquals(26.0, forecastItem.getTemperature());
        assertEquals(63.0, forecastItem.getHumidity());

        verify(restTemplate).getForObject(expectedUrl, WeatherForecastDto.class);
    }

    @Test
    void shouldThrowExceptionWhenRabbitMQFails() {
        // Given
        WeatherDataDto weatherData = new WeatherDataDto(
                "station-1",
                Instant.now(),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        doThrow(new RuntimeException("RabbitMQ error"))
                .when(rabbitTemplate).convertAndSend(eq(WEATHER_DATA_EXCHANGE), eq(WEATHER_DATA_ROUTING_KEY), any(WeatherMessage.class));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> weatherService.processWeatherData(weatherData));

        assertEquals("Failed to process weather data", exception.getMessage());
        assertTrue(exception.getCause().getMessage().contains("RabbitMQ error"));
    }

    @Test
    void shouldThrowExceptionWhenRestTemplateFails() {
        // Given
        String stationId = "station-1";
        int hours = 3;

        String expectedUrl = String.format("http://localhost:8081/api/v1/weather/forecast?stationId=%s&hours=%d", stationId, hours);

        when(restTemplate.getForObject(expectedUrl, WeatherForecastDto.class))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> weatherService.getWeatherForecast(stationId, hours));

        assertEquals("Failed to get weather forecast", exception.getMessage());
        assertTrue(exception.getCause().getMessage().contains("Service unavailable"));
    }

    private WeatherForecastDto createTestForecast() {
        WeatherForecastDto.ForecastItem forecastItem = new WeatherForecastDto.ForecastItem(
                Instant.now().plusSeconds(3600), // +1 hour
                26.0,
                63.0,
                1013.5,
                0.0
        );

        return new WeatherForecastDto(
                "station-1",
                Instant.now(),
                List.of(forecastItem)
        );
    }
}