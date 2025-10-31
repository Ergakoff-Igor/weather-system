package com.weather.processing.repository;

import com.weather.processing.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    List<WeatherData> findByStationIdOrderByTimestampDesc(String stationId);

    @Query("SELECT w FROM WeatherData w WHERE w.stationId = :stationId AND w.timestamp >= :since ORDER BY w.timestamp DESC")
    List<WeatherData> findByStationIdAndTimestampAfterOrderByTimestampDesc(
            @Param("stationId") String stationId,
            @Param("since") LocalDateTime since);

    @Query(value = "SELECT * FROM weather_data WHERE station_id = :stationId ORDER BY timestamp DESC LIMIT :limit",
            nativeQuery = true)
    List<WeatherData> findLatestByStationId(@Param("stationId") String stationId, @Param("limit") int limit);

    boolean existsByStationIdAndTimestamp(String stationId, LocalDateTime timestamp);
}