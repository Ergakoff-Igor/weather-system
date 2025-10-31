package com.weather.processing.controller;

import com.weather.processing.service.ForecastService;
import com.weather.shared.dto.WeatherForecastDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "Weather Forecast API", description = "API для получения прогнозов погоды")
public class WeatherForecastController {

    private final ForecastService forecastService;

    @GetMapping("/forecast")
    @Operation(summary = "Получение прогноза погоды", description = "Генерирует прогноз погоды для указанной станции")
    public WeatherForecastDto getWeatherForecast(
            @RequestParam String stationId,
            @RequestParam(defaultValue = "1") int hours) {

        log.info("Requesting forecast for station: {}, hours: {}", stationId, hours);
        return forecastService.generateForecast(stationId, hours);
    }
}