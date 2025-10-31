package com.weather.processing.integration;

import com.weather.processing.config.TestCacheConfig;
import com.weather.processing.entity.WeatherData;
import com.weather.processing.repository.WeatherDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Import(TestCacheConfig.class) // Импортируем тестовую конфигурацию кэша
class WeatherProcessingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Отключаем кэш для тестов если нужно
        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired
    private WeatherDataRepository weatherDataRepository;

    @Test
    void shouldSaveAndRetrieveWeatherData() {
        // Given
        WeatherData weatherData = new WeatherData();
        weatherData.setStationId("test-station");
        weatherData.setTimestamp(Instant.now());
        weatherData.setTemperature(25.5);
        weatherData.setHumidity(65.0);
        weatherData.setPressure(1013.25);
        weatherData.setPrecipitation(0.0);

        // When
        WeatherData saved = weatherDataRepository.save(weatherData);
        List<WeatherData> found = weatherDataRepository.findByStationIdOrderByTimestampDesc("test-station");

        // Then
        assertNotNull(saved.getId());
        assertEquals(1, found.size());
        assertEquals("test-station", found.get(0).getStationId());
        assertEquals(25.5, found.get(0).getTemperature());
    }

    @Test
    void shouldFindLatestWeatherData() {
        // Given
        String stationId = "station-latest";

        WeatherData olderData = new WeatherData();
        olderData.setStationId(stationId);
        olderData.setTimestamp(Instant.now().minus(2, ChronoUnit.HOURS));
        olderData.setTemperature(20.0);
        olderData.setHumidity(60.0);
        olderData.setPressure(1010.0);
        olderData.setPrecipitation(0.0);

        WeatherData newerData = new WeatherData();
        newerData.setStationId(stationId);
        newerData.setTimestamp(Instant.now().minus(1, ChronoUnit.HOURS));
        newerData.setTemperature(22.0);
        newerData.setHumidity(62.0);
        newerData.setPressure(1012.0);
        newerData.setPrecipitation(0.0);

        weatherDataRepository.save(olderData);
        weatherDataRepository.save(newerData);

        // When
        List<WeatherData> latest = weatherDataRepository.findLatestByStationId(stationId, 5);

        // Then
        assertEquals(2, latest.size());
        // Should be ordered by timestamp descending
        assertEquals(22.0, latest.get(0).getTemperature()); // Newer first
        assertEquals(20.0, latest.get(1).getTemperature()); // Older second
    }
}