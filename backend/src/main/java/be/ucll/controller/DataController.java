package be.ucll.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.ucll.model.SensorReading;
import be.ucll.service.SensorService;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final SensorService sensorService;

    public DataController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    // ESP32 posts data here:
    // POST /api/data
    // Body: { "deviceId": "esp32-team1", "temperature": 22.4, "isMoving": false }
    @PostMapping
    public ResponseEntity<SensorReading> receiveData(@RequestBody SensorReading reading) {
        SensorReading saved = sensorService.save(reading);
        return ResponseEntity.ok(saved);
    }

    // Frontend fetches all readings here:
    // GET /api/data
    @GetMapping
    public ResponseEntity<List<SensorReading>> getAllData() {
        return ResponseEntity.ok(sensorService.getAll());
    }

    // Frontend fetches latest reading here:
    // GET /api/data/latest
    @GetMapping("/latest")
    public ResponseEntity<SensorReading> getLatest() {
        return ResponseEntity.ok(sensorService.getLatest());
    }
}