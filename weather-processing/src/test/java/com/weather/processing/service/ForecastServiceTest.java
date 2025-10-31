package com.weather.processing.service;

import com.weather.processing.entity.WeatherData;
import com.weather.shared.dto.WeatherForecastDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private WeatherDataService weatherDataService;

    private ForecastService forecastService;

    @BeforeEach
    void setUp() {
        forecastService = new ForecastService(weatherDataService);
        // Используем ReflectionTestUtils для установки приватных полей
        ReflectionTestUtils.setField(forecastService, "historySize", 15);
        ReflectionTestUtils.setField(forecastService, "maxForecastHours", 24);
    }

    @Test
    void shouldGenerateForecastSuccessfully() {
        // Given
        String stationId = "station-1";
        int hours = 3;
        List<WeatherData> historicalData = createHistoricalData();

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(historicalData);

        // When
        WeatherForecastDto forecast = forecastService.generateForecast(stationId, hours);

        // Then
        assertNotNull(forecast);
        assertEquals(stationId, forecast.getStationId());
        assertEquals(hours, forecast.getForecasts().size());
        assertNotNull(forecast.getGeneratedAt());

        // Verify forecast items have reasonable values
        forecast.getForecasts().forEach(item -> {
            assertTrue(item.getTemperature() >= -50 && item.getTemperature() <= 50);
            assertTrue(item.getHumidity() >= 0 && item.getHumidity() <= 100);
            assertTrue(item.getPressure() >= 900 && item.getPressure() <= 1100);
            assertTrue(item.getPrecipitation() >= 0);
        });
    }

    @Test
    void shouldThrowExceptionWhenNotEnoughHistoricalData() {
        // Given
        String stationId = "station-1";
        int hours = 3;

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(List.of(createWeatherData(1))); // Only one data point

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forecastService.generateForecast(stationId, hours));

        assertEquals("Not enough historical data for station: " + stationId, exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenInvalidHours() {
        // Given
        String stationId = "station-1";
        int invalidHours = 48;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forecastService.generateForecast(stationId, invalidHours));

        assertEquals("Hours must be between 1 and 24", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenZeroHours() {
        // Given
        String stationId = "station-1";
        int invalidHours = 0;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forecastService.generateForecast(stationId, invalidHours));

        assertEquals("Hours must be between 1 and 24", exception.getMessage());
    }

    @Test
    void shouldCalculateTrendCorrectly() {
        // Given
        String stationId = "station-1";
        int hours = 1;

        // Data with clear trend: temperature increasing by 1 degree per hour
        List<WeatherData> historicalData = Arrays.asList(
                createWeatherDataWithValues(0, 22.0, 60.0, 1010.0, 0.0),
                createWeatherDataWithValues(1, 21.0, 62.0, 1009.0, 0.0),
                createWeatherDataWithValues(2, 20.0, 64.0, 1008.0, 0.0)
        );

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(historicalData);

        // When
        WeatherForecastDto forecast = forecastService.generateForecast(stationId, hours);

        // Then
        assertNotNull(forecast);
        assertEquals(1, forecast.getForecasts().size());

        WeatherForecastDto.ForecastItem firstForecast = forecast.getForecasts().get(0);
        // Should continue the trend (approximately)
        assertTrue(firstForecast.getTemperature() > 22.0); // Trending up
    }

    @Test
    void shouldHandleSingleDataPointGracefully() {
        // Given
        String stationId = "station-1";
        int hours = 1;

        // Only one data point - should use default values for trend calculation
        List<WeatherData> historicalData = List.of(
                createWeatherDataWithValues(0, 22.0, 60.0, 1010.0, 0.0)
        );

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(historicalData);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> forecastService.generateForecast(stationId, hours));

        assertEquals("Not enough historical data for station: " + stationId, exception.getMessage());
    }

    @Test
    void shouldGenerateMultipleHoursForecast() {
        // Given
        String stationId = "station-1";
        int hours = 5;
        List<WeatherData> historicalData = createHistoricalData();

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(historicalData);

        // When
        WeatherForecastDto forecast = forecastService.generateForecast(stationId, hours);

        // Then
        assertNotNull(forecast);
        assertEquals(hours, forecast.getForecasts().size());

        // Verify each forecast hour has increasing timestamp
        Instant previousTimestamp = null;
        for (WeatherForecastDto.ForecastItem item : forecast.getForecasts()) {
            assertNotNull(item.getTimestamp());
            if (previousTimestamp != null) {
                assertTrue(item.getTimestamp().isAfter(previousTimestamp));
            }
            previousTimestamp = item.getTimestamp();
        }
    }

    private List<WeatherData> createHistoricalData() {
        return Arrays.asList(
                createWeatherData(0),
                createWeatherData(1),
                createWeatherData(2),
                createWeatherData(3),
                createWeatherData(4)
        );
    }

    private WeatherData createWeatherData(int hoursAgo) {
        WeatherData data = new WeatherData();
        data.setStationId("station-1");
        data.setTimestamp(Instant.now().minus(hoursAgo, ChronoUnit.HOURS));
        data.setTemperature(20.0 + hoursAgo); // Increasing trend
        data.setHumidity(60.0 - hoursAgo); // Decreasing trend
        data.setPressure(1013.0 + hoursAgo * 0.1); // Slowly increasing
        data.setPrecipitation(hoursAgo % 3 == 0 ? 1.0 : 0.0); // Some precipitation
        return data;
    }

    private WeatherData createWeatherDataWithValues(int hoursAgo, double temp, double humidity, double pressure, double precipitation) {
        WeatherData data = new WeatherData();
        data.setStationId("station-1");
        data.setTimestamp(Instant.now().minus(hoursAgo, ChronoUnit.HOURS));
        data.setTemperature(temp);
        data.setHumidity(humidity);
        data.setPressure(pressure);
        data.setPrecipitation(precipitation);
        return data;
    }
}