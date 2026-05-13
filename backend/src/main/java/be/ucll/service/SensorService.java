package be.ucll.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.ucll.model.SensorReading;
import be.ucll.repository.SensorReadingRepository;

@Service
public class SensorService {

    private final SensorReadingRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SensorService(SensorReadingRepository repository) {
        this.repository = repository;
    }

    public SensorReading save(SensorReading reading) {
        return repository.save(reading);
    }

    public List<SensorReading> getAll() {
        return repository.findAll();
    }

    public SensorReading getLatest() {
        return repository.findTopByOrderByTimestampDesc();
    }

    // Called by WebSocket handler when ESP32 sends sensor data as JSON string
    public SensorReading saveFromJson(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);

        String deviceId = node.has("deviceId") ? node.get("deviceId").asText() : "unknown";
        Double temperature = node.has("temperature") ? node.get("temperature").asDouble() : null;
        Boolean isMoving = node.has("isMoving") ? node.get("isMoving").asBoolean() : null;

        SensorReading reading = new SensorReading(deviceId, temperature, isMoving);
        return repository.save(reading);
    }
}