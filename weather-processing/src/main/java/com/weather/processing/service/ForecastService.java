package com.weather.processing.service;

import com.weather.processing.entity.WeatherData;
import com.weather.shared.dto.WeatherForecastDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
            log.warn("Not enough historical data for station: {}. Available: {}. Generating test forecast.",
                    stationId, historicalData.size());
            return generateTestForecast(stationId, hours, historicalData);
        }

        List<WeatherForecastDto.ForecastItem> forecasts = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 1; i <= hours; i++) {
            Instant forecastTime = now.plus(i, ChronoUnit.HOURS);
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

    private WeatherForecastDto generateTestForecast(String stationId, int hours, List<WeatherData> availableData) {
        List<WeatherForecastDto.ForecastItem> forecasts = new ArrayList<>();
        Instant now = Instant.now();

        double baseTemp = 20.0;
        double baseHumidity = 60.0;
        double basePressure = 1013.0;
        double basePrecipitation = 0.0;

        if (!availableData.isEmpty()) {
            WeatherData latest = availableData.get(0);
            baseTemp = latest.getTemperature();
            baseHumidity = latest.getHumidity();
            basePressure = latest.getPressure();
            basePrecipitation = latest.getPrecipitation();
        }

        for (int i = 1; i <= hours; i++) {
            Instant forecastTime = now.plus(i, ChronoUnit.HOURS);

            double temp = baseTemp + (i * 0.5);
            double humidity = Math.max(30, baseHumidity - (i * 2));
            double pressure = basePressure + (i * 0.1);
            double precipitation = basePrecipitation;

            forecasts.add(new WeatherForecastDto.ForecastItem(
                    forecastTime,
                    Math.round(temp * 10.0) / 10.0,
                    Math.round(humidity * 10.0) / 10.0,
                    Math.round(pressure * 10.0) / 10.0,
                    Math.round(precipitation * 10.0) / 10.0
            ));
        }

        WeatherForecastDto forecast = new WeatherForecastDto();
        forecast.setStationId(stationId);
        forecast.setGeneratedAt(now);
        forecast.setForecasts(forecasts);

        log.info("Generated test forecast for station: {} with {} hours", stationId, hours);
        return forecast;
    }

    private WeatherForecastDto.ForecastItem calculateForecast(List<WeatherData> historicalData,
                                                              Instant forecastTime, int hoursAhead) {
        int n = historicalData.size();

        WeatherData latest = historicalData.get(0);
        WeatherData previous = historicalData.get(Math.min(1, n - 1));

        double timeDiffHours = Math.max(1.0,
                java.time.Duration.between(previous.getTimestamp(), latest.getTimestamp()).toHours());

        double tempTrend = (latest.getTemperature() - previous.getTemperature()) / timeDiffHours;
        double humidityTrend = (latest.getHumidity() - previous.getHumidity()) / timeDiffHours;
        double pressureTrend = (latest.getPressure() - previous.getPressure()) / timeDiffHours;
        double precipitationTrend = (latest.getPrecipitation() - previous.getPrecipitation()) / timeDiffHours;

        double forecastTemp = latest.getTemperature() + (tempTrend * hoursAhead);
        double forecastHumidity = latest.getHumidity() + (humidityTrend * hoursAhead);
        double forecastPressure = latest.getPressure() + (pressureTrend * hoursAhead);
        double forecastPrecipitation = Math.max(0, latest.getPrecipitation() + (precipitationTrend * hoursAhead));

        forecastTemp = Math.max(-50, Math.min(50, forecastTemp));
        forecastHumidity = Math.max(0, Math.min(100, forecastHumidity));
        forecastPressure = Math.max(900, Math.min(1100, forecastPressure));
        forecastPrecipitation = Math.max(0, Math.min(100, forecastPrecipitation));

        return new WeatherForecastDto.ForecastItem(
                forecastTime,
                Math.round(forecastTemp * 10.0) / 10.0,
                Math.round(forecastHumidity * 10.0) / 10.0,
                Math.round(forecastPressure * 10.0) / 10.0,
                Math.round(forecastPrecipitation * 10.0) / 10.0
        );
    }
}