package com.weather.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class WeatherProcessingApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeatherProcessingApplication.class, args);
    }
}