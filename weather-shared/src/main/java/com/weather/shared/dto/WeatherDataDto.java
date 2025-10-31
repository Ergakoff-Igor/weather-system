package com.weather.shared.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataDto {

    @NotBlank(message = "stationId is required")
    private String stationId;

    @NotNull(message = "timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;

    @NotNull(message = "temperature is required")
    @DecimalMin(value = "-100.0", message = "Temperature must be >= -100")
    @DecimalMax(value = "100.0", message = "Temperature must be <= 100")
    private Double temperature;

    @NotNull(message = "humidity is required")
    @DecimalMin(value = "0.0", message = "Humidity must be >= 0")
    @DecimalMax(value = "100.0", message = "Humidity must be <= 100")
    private Double humidity;

    @NotNull(message = "pressure is required")
    @DecimalMin(value = "800.0", message = "Pressure must be >= 800")
    @DecimalMax(value = "1200.0", message = "Pressure must be <= 1200")
    private Double pressure;

    @NotNull(message = "precipitation is required")
    @DecimalMin(value = "0.0", message = "Precipitation must be >= 0")
    private Double precipitation;
}