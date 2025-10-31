package com.weather.processing.service;

import com.weather.processing.entity.WeatherData;
import com.weather.shared.dto.WeatherForecastDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForecastService {

    private final WeatherDataService weatherDataService;

    @Value("${weather.forecast.history-size:15}")
    private int historySize;

    @Value("${weather.forecast.max-forecast-hours:24}")
    private int maxForecastHours;

    @Cacheable(value = "weatherForecasts", key = "#stationId + '_' + #hours")
    public WeatherForecastDto generateForecast(String stationId, int hours) {
        log.info("Generating forecast for station: {}, hours: {}", stationId, hours);

        if (hours <= 0 || hours > maxForecastHours) {
            throw new IllegalArgumentException("Hours must be between 1 and " + maxForecastHours);
        }

        List<WeatherData> historicalData = weatherDataService.getLatestWeatherData(stationId, historySize);

        if (historicalData.size() < 2) {
            throw new IllegalArgumentException("Not enough historical data for station: " + stationId);
        }

        List<WeatherForecastDto.ForecastItem> forecasts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= hours; i++) {
            LocalDateTime forecastTime = now.plusHours(i);
            WeatherForecastDto.ForecastItem forecastItem = calculateForecast(historicalData, forecastTime, i);
            forecasts.add(forecastItem);
        }

        WeatherForecastDto forecast = new WeatherForecastDto();
        forecast.setStationId(stationId);
        forecast.setGeneratedAt(now);
        forecast.setForecasts(forecasts);

        log.debug("Generated forecast with {} hours for station: {}", hours, stationId);
        return forecast;
    }

    private WeatherForecastDto.ForecastItem calculateForecast(List<WeatherData> historicalData,
                                                              LocalDateTime forecastTime, int hoursAhead) {
        // Простая линейная экстраполяция на основе последних данных
        int n = historicalData.size();

        // Берем последние данные для экстраполяции
        WeatherData latest = historicalData.get(0);
        WeatherData previous = historicalData.get(Math.min(1, n - 1));

        // Временной интервал между последними двумя записями (в часах)
        double timeDiffHours = Math.max(1.0,
                java.time.Duration.between(previous.getTimestamp(), latest.getTimestamp()).toHours());

        // Рассчитываем тренды
        double tempTrend = (latest.getTemperature() - previous.getTemperature()) / timeDiffHours;
        double humidityTrend = (latest.getHumidity() - previous.getHumidity()) / timeDiffHours;
        double pressureTrend = (latest.getPressure() - previous.getPressure()) / timeDiffHours;
        double precipitationTrend = (latest.getPrecipitation() - previous.getPrecipitation()) / timeDiffHours;

        // Применяем тренд для прогноза
        double forecastTemp = latest.getTemperature() + (tempTrend * hoursAhead);
        double forecastHumidity = latest.getHumidity() + (humidityTrend * hoursAhead);
        double forecastPressure = latest.getPressure() + (pressureTrend * hoursAhead);
        double forecastPrecipitation = Math.max(0, latest.getPrecipitation() + (precipitationTrend * hoursAhead));

        // Ограничиваем значения в разумных пределах
        forecastTemp = Math.max(-50, Math.min(50, forecastTemp));
        forecastHumidity = Math.max(0, Math.min(100, forecastHumidity));
        forecastPressure = Math.max(900, Math.min(1100, forecastPressure));
        forecastPrecipitation = Math.max(0, Math.min(100, forecastPrecipitation));

        return new WeatherForecastDto.ForecastItem(
                forecastTime,
                Math.round(forecastTemp * 10.0) / 10.0, // Округляем до 1 знака
                Math.round(forecastHumidity * 10.0) / 10.0,
                Math.round(forecastPressure * 10.0) / 10.0,
                Math.round(forecastPrecipitation * 10.0) / 10.0
        );
    }
}