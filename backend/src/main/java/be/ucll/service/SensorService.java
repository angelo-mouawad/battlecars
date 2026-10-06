package be.ucll.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import be.ucll.controller.dto.SensorReadingRequest;
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

    public SensorReading save(SensorReadingRequest request) {
        Map<String, Object> data = request.data() == null ? Map.of() : request.data();
        String event = request.event() == null || request.event().isBlank()
                ? eventFor(objectMapper.valueToTree(data))
                : request.event().trim();
        try {
            return repository.save(new SensorReading(request.carId(), event, objectMapper.writeValueAsString(data)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Sensor data could not be stored as JSON");
        }
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

    public SensorReading saveFromJson(String json) {
        return saveInternal(json, null);
    }

    public SensorReading saveFromJsonWithMatch(String json, Long matchId) {
        return saveInternal(json, matchId);
    }

    private SensorReading saveInternal(String json, Long matchId) {
        ObjectNode node = parseObject(json);

        JsonNode carNode = node.get("carId");
        if (carNode == null || !carNode.canConvertToInt() || (carNode.asInt() != 1 && carNode.asInt() != 2)) {
            throw new IllegalArgumentException("Sensor message needs a carId of 1 or 2");
        }
        int carId = carNode.asInt();

        String event = node.hasNonNull("event") && !node.get("event").asText().isBlank()
                ? node.get("event").asText().trim()
                : eventFor(node);

        ObjectNode dataNode = node.deepCopy();
        dataNode.remove("carId");
        dataNode.remove("event");

        String data;
        try {
            data = objectMapper.writeValueAsString(dataNode);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Sensor data could not be stored as JSON");
        }

        SensorReading reading = matchId != null
                ? new SensorReading(carId, event, data, matchId)
                : new SensorReading(carId, event, data);
        return repository.save(reading);
    }

    private ObjectNode parseObject(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Sensor message is empty");
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node instanceof ObjectNode objectNode) {
                return objectNode;
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Sensor message is not valid JSON");
        }
        throw new IllegalArgumentException("Sensor message must be a JSON object");
    }

    private static String eventFor(JsonNode node) {
        if (node.path("hit").asBoolean(false)) {
            return "hit";
        }
        if (node.has("orientation")) {
            return "orientation";
        }
        return "unknown";
    }
}
