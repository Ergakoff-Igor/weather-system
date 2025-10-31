package com.weather.processing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weather_data", indexes = {
        @Index(name = "idx_station_timestamp", columnList = "stationId, timestamp"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stationId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private Double humidity;

    @Column(nullable = false)
    private Double pressure;

    @Column(nullable = false)
    private Double precipitation;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}