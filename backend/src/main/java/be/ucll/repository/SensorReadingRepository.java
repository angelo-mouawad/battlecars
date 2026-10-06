package be.ucll.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import be.ucll.model.SensorReading;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    // Latest reading overall
    SensorReading findTopByOrderByTimestampDesc();

    // All readings for a specific car
    List<SensorReading> findByCarId(Integer carId);

    // All readings for a specific event type
    List<SensorReading> findByEvent(String event);

    // All readings for a specific car and event type
    List<SensorReading> findByCarIdAndEvent(Integer carId, String event);

    // Latest reading for a specific car
    @Query(value = "SELECT * FROM sensor_readings WHERE car_id = :carId ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    SensorReading findLatestByCarId(@Param("carId") Integer carId);

    // Hit events only, clean jsonb query, no casting needed
    @Query(value = "SELECT * FROM sensor_readings WHERE data->>'hit' = 'true'", nativeQuery = true)
    List<SensorReading> findAllHits();

    // Heavy hits only
    @Query(value = "SELECT * FROM sensor_readings WHERE data->>'hitStrength' = 'heavy'", nativeQuery = true)
    List<SensorReading> findHeavyHits();
}