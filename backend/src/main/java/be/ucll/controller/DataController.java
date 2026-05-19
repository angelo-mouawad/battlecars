package be.ucll.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // POST /api/data
    // Alternative REST endpoint — ESP32 can POST directly without WebSocket
    @PostMapping
    public ResponseEntity<SensorReading> receiveData(@RequestBody SensorReading reading) {
        SensorReading saved = sensorService.save(reading);
        return ResponseEntity.ok(saved);
    }

    // GET /api/data
    @GetMapping
    public ResponseEntity<List<SensorReading>> getAllData() {
        return ResponseEntity.ok(sensorService.getAll());
    }

    // GET /api/data/latest
    @GetMapping("/latest")
    public ResponseEntity<SensorReading> getLatest() {
        return ResponseEntity.ok(sensorService.getLatest());
    }

    // GET /api/data/car/1
    // GET /api/data/car/2
    @GetMapping("/car/{carId}")
    public ResponseEntity<List<SensorReading>> getByCarId(@PathVariable Integer carId) {
        return ResponseEntity.ok(sensorService.getByCarId(carId));
    }

    // GET /api/data/car/1/latest
    @GetMapping("/car/{carId}/latest")
    public ResponseEntity<SensorReading> getLatestByCarId(@PathVariable Integer carId) {
        return ResponseEntity.ok(sensorService.getLatestByCarId(carId));
    }

    // GET /api/data/event/hit
    // GET /api/data/event/move
    // GET /api/data/event/speed
    // GET /api/data/event/stop
    @GetMapping("/event/{event}")
    public ResponseEntity<List<SensorReading>> getByEvent(@PathVariable String event) {
        return ResponseEntity.ok(sensorService.getByEvent(event));
    }

    // GET /api/data/car/1/event/hit
    @GetMapping("/car/{carId}/event/{event}")
    public ResponseEntity<List<SensorReading>> getByCarIdAndEvent(
            @PathVariable Integer carId,
            @PathVariable String event) {
        return ResponseEntity.ok(sensorService.getByCarIdAndEvent(carId, event));
    }

    // GET /api/data/hits — all hits across both cars
    @GetMapping("/hits")
    public ResponseEntity<List<SensorReading>> getHits() {
        return ResponseEntity.ok(sensorService.getHits());
    }

    // GET /api/data/hits/heavy — heavy hits only
    @GetMapping("/hits/heavy")
    public ResponseEntity<List<SensorReading>> getHeavyHits() {
        return ResponseEntity.ok(sensorService.getHeavyHits());
    }
}