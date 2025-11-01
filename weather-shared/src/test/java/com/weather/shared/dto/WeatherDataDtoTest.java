package com.weather.shared.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeatherDataDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldCreateValidWeatherDataDto() {
        // Given
        WeatherDataDto dto = new WeatherDataDto(
                "station-1",
                Instant.now(),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        // When
        Set<ConstraintViolation<WeatherDataDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailValidationWhenStationIdIsBlank() {
        // Given
        WeatherDataDto dto = new WeatherDataDto(
                "",
                Instant.now(),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        // When
        Set<ConstraintViolation<WeatherDataDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertEquals("stationId is required", violations.iterator().next().getMessage());
    }

    @Test
    void shouldFailValidationWhenTemperatureOutOfRange() {
        // Given
        WeatherDataDto dto = new WeatherDataDto(
                "station-1",
                Instant.now(),
                -150.0, // Too low
                65.0,
                1013.25,
                0.0
        );

        // When
        Set<ConstraintViolation<WeatherDataDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Temperature must be"));
    }

    @Test
    void shouldFailValidationWhenHumidityOutOfRange() {
        // Given
        WeatherDataDto dto = new WeatherDataDto(
                "station-1",
                Instant.now(),
                25.5,
                150.0, // Too high
                1013.25,
                0.0
        );

        // When
        Set<ConstraintViolation<WeatherDataDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Humidity must be"));
    }

    @Test
    void shouldFailValidationWhenPressureOutOfRange() {
        // Given
        WeatherDataDto dto = new WeatherDataDto(
                "station-1",
                Instant.now(),
                25.5,
                65.0,
                500.0, // Too low
                0.0
        );

        // When
        Set<ConstraintViolation<WeatherDataDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Pressure must be"));
    }

    @Test
    void shouldFailValidationWhenPrecipitationNegative() {
        // Given
        WeatherDataDto dto = new WeatherDataDto(
                "station-1",
                Instant.now(),
                25.5,
                65.0,
                1013.25,
                -5.0 // Negative
        );

        // When
        Set<ConstraintViolation<WeatherDataDto>> violations = validator.validate(dto);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Precipitation must be"));
    }
}