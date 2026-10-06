package be.ucll.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_id", nullable = false)
    private Integer carId;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Which match this reading belongs to, null if received outside an active match
    @Column(name = "match_id")
    private Long matchId;

    // All sensor fields stored as native PostgreSQL jsonb.
    // New ESP32 fields flow in automatically without schema changes.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    public SensorReading() {}

    public SensorReading(Integer carId, String event, String data) {
        this.carId = carId;
        this.event = event;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public SensorReading(Integer carId, String event, String data, Long matchId) {
        this(carId, event, data);
        this.matchId = matchId;
    }

    public Long getId() { return id; }

    public Integer getCarId() { return carId; }
    public void setCarId(Integer carId) { this.carId = carId; }

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}