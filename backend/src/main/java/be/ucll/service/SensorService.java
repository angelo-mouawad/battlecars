package be.ucll.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    public List<SensorReading> getByCarId(Integer carId) {
        return repository.findByCarId(carId);
    }

    public SensorReading getLatestByCarId(Integer carId) {
        return repository.findLatestByCarId(carId);
    }

    public List<SensorReading> getByEvent(String event) {
        return repository.findByEvent(event);
    }

    public List<SensorReading> getByCarIdAndEvent(Integer carId, String event) {
        return repository.findByCarIdAndEvent(carId, event);
    }

    public List<SensorReading> getHits() {
        return repository.findAllHits();
    }

    public List<SensorReading> getHeavyHits() {
        return repository.findHeavyHits();
    }

    // Called by WebSocket handler when ESP32 hub sends sensor data as JSON string.
    // carId, event, deviceId, isMoving, and temperature are extracted as fixed columns for easy querying.
    // All remaining fields are stored as jsonb — new ESP32 fields flow in automatically.
    public SensorReading saveFromJson(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);

        Integer carId = node.has("carId") ? node.get("carId").asInt() : null;
        String event = node.has("event") ? node.get("event").asText() : "unknown";
        Integer deviceId = node.has("deviceId") ? node.get("deviceId").asInt() : null;
        Boolean isMoving = node.has("isMoving") ? node.get("isMoving").asBoolean() : null;
        Integer temperature = node.has("temperature") ? node.get("temperature").asInt() : null;

        // Strip fixed columns — stored as dedicated columns, not duplicated in jsonb
        ObjectNode dataNode = ((ObjectNode) node).deepCopy();
        dataNode.remove("carId");
        dataNode.remove("event");
        dataNode.remove("deviceId");
        dataNode.remove("isMoving");
        dataNode.remove("temperature");

        String data = objectMapper.writeValueAsString(dataNode);

        SensorReading reading = new SensorReading(carId, event, data, deviceId, isMoving, temperature);
        return repository.save(reading);
    }
}