package com.weather.processing.service;

import com.weather.processing.entity.WeatherData;
import com.weather.processing.repository.WeatherDataRepository;
import com.weather.shared.messaging.WeatherMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDataService {

    private final WeatherDataRepository weatherDataRepository;

    @Transactional
    public void saveWeatherData(WeatherMessage message) {
        // Проверяем, нет ли уже таких данных
        if (!weatherDataRepository.existsByStationIdAndTimestamp(message.getStationId(), message.getTimestamp())) {
            WeatherData weatherData = new WeatherData();
            weatherData.setStationId(message.getStationId());
            weatherData.setTimestamp(message.getTimestamp());
            weatherData.setTemperature(message.getTemperature());
            weatherData.setHumidity(message.getHumidity());
            weatherData.setPressure(message.getPressure());
            weatherData.setPrecipitation(message.getPrecipitation());

            weatherDataRepository.save(weatherData);
            log.info("Saved weather data for station: {}, timestamp: {}",
                    message.getStationId(), message.getTimestamp());
        } else {
            log.warn("Weather data already exists for station: {}, timestamp: {}",
                    message.getStationId(), message.getTimestamp());
        }
    }

    public List<WeatherData> getLatestWeatherData(String stationId, int limit) {
        return weatherDataRepository.findLatestByStationId(stationId, limit);
    }

    @Transactional(readOnly = true)
    public long getDataCount() {
        return weatherDataRepository.count();
    }
}