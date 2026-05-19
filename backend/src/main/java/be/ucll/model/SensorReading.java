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

    // Always queryable — fixed columns
    @Column(name = "car_id", nullable = false)
    private Integer carId;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // All other sensor fields stored as native PostgreSQL jsonb.
    // jsonb = binary JSON — faster queries, validated on insert, no casting needed.
    // New ESP32 fields flow in automatically without any schema changes.
    // e.g. {"isMoving":true,"speed":"fast","hit":false,"hitStrength":"none","orientation":"forward"}
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    public SensorReading() {}

    public SensorReading(Integer carId, String event, String data) {
        this.carId = carId;
        this.event = event;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Integer getCarId() { return carId; }
    public void setCarId(Integer carId) { this.carId = carId; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}