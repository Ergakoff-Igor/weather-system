package com.weather.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecastDto {

    private String stationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant generatedAt;

    private List<ForecastItem> forecasts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastItem {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        private Instant timestamp;
        private Double temperature;
        private Double humidity;
        private Double pressure;
        private Double precipitation;
    }
}