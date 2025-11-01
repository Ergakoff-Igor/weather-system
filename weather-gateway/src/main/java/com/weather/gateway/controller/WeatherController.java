package com.weather.gateway.controller;

import com.weather.gateway.service.WeatherService;
import com.weather.shared.dto.WeatherDataDto;
import com.weather.shared.dto.WeatherForecastDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "Weather API", description = "API для сбора данных и получения прогноза погоды")
public class WeatherController {

    private final WeatherService weatherService;

    @PostMapping("/data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Отправка данных о погоде", description = "Принимает данные от погодных станций")
    public void receiveWeatherData(@Valid @RequestBody WeatherDataDto weatherData) {
        log.info("Received weather data from station: {}", weatherData.getStationId());
        weatherService.processWeatherData(weatherData);
    }

    @GetMapping("/forecast")
    @Operation(summary = "Получение прогноза погоды", description = "Возвращает прогноз погоды для указанной станции")
    public WeatherForecastDto getWeatherForecast(
            @RequestParam("stationId") @NotBlank String stationId,
            @RequestParam(value = "hours", defaultValue = "1")
            @Min(value = 1, message = "Hours must be at least 1")
            @Max(value = 24, message = "Hours cannot exceed 24") int hours) {

        log.info("Getting weather forecast for station: {}, hours: {}", stationId, hours);
        return weatherService.getWeatherForecast(stationId, hours);
    }
}