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

import java.time.LocalDateTime;
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
                LocalDateTime.now(),
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
    void shouldReturnBadRequestForInvalidData() throws Exception {
        // Given - invalid JSON structure
        String invalidJson = "{}"; // Пустой JSON без обязательных полей

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
                .andExpect(jsonPath("$.forecasts.length()").value(1));

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
                .andExpect(status().isBadRequest()); // Spring вернет 400 когда обязательный параметр отсутствует

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
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Service unavailable"));
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
                List.of(forecastItem)
        );
    }

    @Test
    void shouldReturnBadRequestForInvalidHours() throws Exception {
        // When & Then - hours=0 (invalid, less than minimum)
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "station-1")
                        .param("hours", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(weatherService, never()).getWeatherForecast(anyString(), anyInt());
    }

    @Test
    void shouldReturnBadRequestForTooManyHours() throws Exception {
        // When & Then - hours=25 (invalid, more than maximum)
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "station-1")
                        .param("hours", "25"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(weatherService, never()).getWeatherForecast(anyString(), anyInt());
    }

    @Test
    void shouldReturnBadRequestForEmptyStationId() throws Exception {
        // When & Then - empty stationId
        mockMvc.perform(get("/api/v1/weather/forecast")
                        .param("stationId", "")
                        .param("hours", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(weatherService, never()).getWeatherForecast(anyString(), anyInt());
    }
}