package be.ucll.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "is_moving")
    private Boolean isMoving;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    // Constructors
    public SensorReading() {}

    public SensorReading(String deviceId, Double temperature, Boolean isMoving) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.isMoving = isMoving;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Boolean getIsMoving() { return isMoving; }
    public void setIsMoving(Boolean isMoving) { this.isMoving = isMoving; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}