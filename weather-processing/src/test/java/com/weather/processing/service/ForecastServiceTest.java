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

        forecast.getForecasts().forEach(item -> {
            assertTrue(item.getTemperature() >= -50 && item.getTemperature() <= 50);
            assertTrue(item.getHumidity() >= 0 && item.getHumidity() <= 100);
            assertTrue(item.getPressure() >= 900 && item.getPressure() <= 1100);
            assertTrue(item.getPrecipitation() >= 0);
        });
    }

    @Test
    void shouldGenerateTestForecastWhenNotEnoughHistoricalData() {
        // Given
        String stationId = "station-1";
        int hours = 3;

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(List.of(createWeatherData(1))); // Only one data point

        // When & Then
        WeatherForecastDto result = forecastService.generateForecast(stationId, hours);

        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        assertEquals(hours, result.getForecasts().size());

        WeatherForecastDto.ForecastItem firstForecast = result.getForecasts().get(0);

        assertTrue(firstForecast.getTemperature() >= 19.0 && firstForecast.getTemperature() <= 23.0);
        assertTrue(firstForecast.getHumidity() >= 55.0 && firstForecast.getHumidity() <= 65.0);
        assertTrue(firstForecast.getPressure() >= 1010.0 && firstForecast.getPressure() <= 1016.0);
    }

    @Test
    void shouldGenerateTestForecastWhenNoHistoricalData() {
        // Given
        String stationId = "station-1";
        int hours = 3;

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(List.of());

        // When & Then
        WeatherForecastDto result = forecastService.generateForecast(stationId, hours);

        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        assertEquals(hours, result.getForecasts().size());

        WeatherForecastDto.ForecastItem firstForecast = result.getForecasts().get(0);
        assertEquals(20.5, firstForecast.getTemperature(), 0.5);
        assertEquals(58.0, firstForecast.getHumidity(), 2.0);
        assertEquals(1013.1, firstForecast.getPressure(), 0.2);
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
        assertTrue(firstForecast.getTemperature() > 22.0); // Trending up
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

        Instant previousTimestamp = null;
        for (WeatherForecastDto.ForecastItem item : forecast.getForecasts()) {
            assertNotNull(item.getTimestamp());
            if (previousTimestamp != null) {
                assertTrue(item.getTimestamp().isAfter(previousTimestamp));
            }
            previousTimestamp = item.getTimestamp();
        }
    }

    @Test
    void shouldUseLatestDataForTestForecast() {
        // Given
        String stationId = "station-1";
        int hours = 2;

        WeatherData latestData = createWeatherDataWithValues(0, 25.5, 65.0, 1013.25, 1.0);

        when(weatherDataService.getLatestWeatherData(eq(stationId), eq(15)))
                .thenReturn(List.of(latestData));

        // When
        WeatherForecastDto result = forecastService.generateForecast(stationId, hours);

        // Then
        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        assertEquals(hours, result.getForecasts().size());

        WeatherForecastDto.ForecastItem firstForecast = result.getForecasts().get(0);
        assertTrue(firstForecast.getTemperature() >= 25.5 && firstForecast.getTemperature() <= 27.0);
        assertTrue(firstForecast.getHumidity() >= 63.0 && firstForecast.getHumidity() <= 65.0);
        assertTrue(firstForecast.getPressure() >= 1013.2 && firstForecast.getPressure() <= 1013.4);
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
        data.setTemperature(20.0 + hoursAgo);
        data.setHumidity(60.0 - hoursAgo);
        data.setPressure(1013.0 + hoursAgo * 0.1);
        data.setPrecipitation(hoursAgo % 3 == 0 ? 1.0 : 0.0);
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