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

    // Called by WebSocket handler — saves without a match context
    public SensorReading saveFromJson(String json) throws Exception {
        return saveFromJson(json, null);
    }

    // Called by WebSocket handler when a match is active — tags the reading with match_id
    public SensorReading saveFromJsonWithMatch(String json, Long matchId) throws Exception {
        return saveFromJson(json, matchId);
    }

    // Core save logic — carId and event extracted as fixed columns,
    // everything else stored as jsonb so new ESP32 fields work automatically
    private SensorReading saveFromJson(String json, Long matchId) throws Exception {
        JsonNode node = objectMapper.readTree(json);

        Integer carId = node.has("carId") ? node.get("carId").asInt() : null;
        String event = node.has("event") ? node.get("event").asText() : "unknown";

        // Strip carId and event — stored as dedicated columns, not duplicated in jsonb
        ObjectNode dataNode = ((ObjectNode) node).deepCopy();
        dataNode.remove("carId");
        dataNode.remove("event");

        String data = objectMapper.writeValueAsString(dataNode);

        SensorReading reading = matchId != null
                ? new SensorReading(carId, event, data, matchId)
                : new SensorReading(carId, event, data);

        return repository.save(reading);
    }
}