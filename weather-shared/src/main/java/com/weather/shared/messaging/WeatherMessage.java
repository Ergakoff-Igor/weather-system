package com.weather.shared.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherMessage {
    private String stationId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;

    private Double temperature;
    private Double humidity;
    private Double pressure;
    private Double precipitation;
}