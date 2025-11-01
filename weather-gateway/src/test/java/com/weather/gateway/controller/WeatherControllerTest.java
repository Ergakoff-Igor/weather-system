package com.weather.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.weather.gateway.service.WeatherService;
import com.weather.shared.dto.WeatherDataDto;
import com.weather.shared.dto.WeatherForecastDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldAcceptWeatherData() throws Exception {
        // Given
        WeatherDataDto weatherData = new WeatherDataDto(
                "station-1",
                Instant.parse("2025-10-31T11:00:00Z"),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        // When & Then
        mockMvc.perform(post("/api/v1/weather/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weatherData)))
                .andExpect(status().isAccepted());

        verify(weatherService).processWeatherData(any(WeatherDataDto.class));
    }

    @Test
    void shouldReturnBadRequestForInvalidWeatherData() throws Exception {
        // Given
        String invalidJson = """
        {
            "stationId": "station-1"
        }
        """;

        // When & Then
        mockMvc.perform(post("/api/v1/weather/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).processWeatherData(any(WeatherDataDto.class));
    }

    @Test
    void shouldReturnBadRequestForNullValues() throws Exception {
        // Given
        String invalidJson = """
            {
                "stationId": "station-1",
                "timestamp": "2025-10-31T11:00:00Z",
                "temperature": null,
                "humidity": 65.0,
                "pressure": 1013.25,
                "precipitation": 0.0
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/weather/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).processWeatherData(any(WeatherDataDto.class));
    }

    @Test
    void shouldGetWeatherForecast() throws Exception {
        // Given
        WeatherForecastDto forecast = createTestForecast();
        when(weatherService.getWeatherForecast(eq("station-1"), eq(3)))
                .thenReturn(forecast);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "station-1")
                        .param("hours", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("station-1"))
                .andExpect(jsonPath("$.forecasts").isArray())
                .andExpect(jsonPath("$.forecasts.length()").value(1))
                .andExpect(jsonPath("$.forecasts[0].temperature").value(26.0))
                .andExpect(jsonPath("$.forecasts[0].humidity").value(63.0));

        verify(weatherService).getWeatherForecast("station-1", 3);
    }

    @Test
    void shouldUseDefaultHoursWhenNotProvided() throws Exception {
        // Given
        WeatherForecastDto forecast = createTestForecast();
        when(weatherService.getWeatherForecast(eq("station-1"), eq(1)))
                .thenReturn(forecast);

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "station-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("station-1"));

        verify(weatherService).getWeatherForecast("station-1", 1);
    }

    @Test
    void shouldReturnBadRequestWhenStationIdMissing() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast"))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).getWeatherForecast(anyString(), anyInt());
    }

    @Test
    void shouldReturnBadRequestForEmptyStationId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "")
                        .param("hours", "3"))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).getWeatherForecast(anyString(), anyInt());
    }

    @Test
    void shouldReturnBadRequestForInvalidHours() throws Exception {
        // When & Then - hours=0 (invalid, less than minimum)
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "station-1")
                        .param("hours", "0"))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).getWeatherForecast(anyString(), anyInt());
    }

    @Test
    void shouldReturnBadRequestForTooManyHours() throws Exception {
        // When & Then - hours=25 (invalid, more than maximum)
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "station-1")
                        .param("hours", "25"))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).getWeatherForecast(anyString(), anyInt());
    }

    @Test
    void shouldHandleServiceException() throws Exception {
        // Given
        when(weatherService.getWeatherForecast(eq("station-1"), eq(3)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "station-1")
                        .param("hours", "3"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldHandleWeatherDataProcessingException() throws Exception {
        // Given
        WeatherDataDto weatherData = new WeatherDataDto(
                "station-1",
                Instant.parse("2025-10-31T11:00:00Z"),
                25.5,
                65.0,
                1013.25,
                0.0
        );

        doThrow(new RuntimeException("RabbitMQ error"))
                .when(weatherService).processWeatherData(any(WeatherDataDto.class));

        // When & Then
        mockMvc.perform(post("/api/v1/weather/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weatherData)))
                .andExpect(status().isInternalServerError());
    }

    private WeatherForecastDto createTestForecast() {
        WeatherForecastDto.ForecastItem forecastItem = new WeatherForecastDto.ForecastItem(
                Instant.parse("2025-10-31T12:00:00Z"), // fixed timestamp for consistent testing
                26.0,
                63.0,
                1013.5,
                0.0
        );

        return new WeatherForecastDto(
                "station-1",
                Instant.parse("2025-10-31T11:00:00Z"), // fixed timestamp
                List.of(forecastItem)
        );
    }
}