package com.weather.processing.service;

import com.weather.processing.entity.WeatherData;
import com.weather.processing.repository.WeatherDataRepository;
import com.weather.shared.messaging.WeatherMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherDataServiceTest {

    @Mock
    private WeatherDataRepository weatherDataRepository;

    @InjectMocks
    private WeatherDataService weatherDataService;

    @Captor
    private ArgumentCaptor<WeatherData> weatherDataCaptor;

    @Test
    void shouldSaveWeatherDataWhenNotExists() {
        // Given
        WeatherMessage message = new WeatherMessage(
                "station-1",
                Instant.now(),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        when(weatherDataRepository.existsByStationIdAndTimestamp(
                eq("station-1"),
                any(Instant.class)))
                .thenReturn(false);

        // When
        weatherDataService.saveWeatherData(message);

        // Then
        verify(weatherDataRepository).save(any(WeatherData.class));
        verify(weatherDataRepository).existsByStationIdAndTimestamp("station-1", message.getTimestamp());
    }

    @Test
    void shouldNotSaveWeatherDataWhenAlreadyExists() {
        // Given
        WeatherMessage message = new WeatherMessage(
                "station-1",
                Instant.now(),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        when(weatherDataRepository.existsByStationIdAndTimestamp(
                eq("station-1"),
                any(Instant.class)))
                .thenReturn(true);

        // When
        weatherDataService.saveWeatherData(message);

        // Then
        verify(weatherDataRepository, never()).save(any(WeatherData.class));
        verify(weatherDataRepository).existsByStationIdAndTimestamp("station-1", message.getTimestamp());
    }

    @Test
    void shouldGetLatestWeatherData() {
        // Given
        String stationId = "station-1";
        int limit = 10;
        List<WeatherData> expectedData = List.of(createWeatherData());

        when(weatherDataRepository.findLatestByStationId(eq(stationId), eq(limit)))
                .thenReturn(expectedData);

        // When
        List<WeatherData> result = weatherDataService.getLatestWeatherData(stationId, limit);

        // Then
        assertEquals(expectedData, result);
        verify(weatherDataRepository).findLatestByStationId(stationId, limit);
    }

    @Test
    void shouldGetDataCount() {
        // Given
        long expectedCount = 42L;
        when(weatherDataRepository.count()).thenReturn(expectedCount);

        // When
        long result = weatherDataService.getDataCount();

        // Then
        assertEquals(expectedCount, result);
        verify(weatherDataRepository).count();
    }

    private WeatherData createWeatherData() {
        WeatherData data = new WeatherData();
        data.setId(1L);
        data.setStationId("station-1");
        data.setTimestamp(Instant.now());
        data.setTemperature(25.5);
        data.setHumidity(65.0);
        data.setPressure(1013.25);
        data.setPrecipitation(0.0);
        return data;
    }
}