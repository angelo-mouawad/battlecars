package be.ucll.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.ucll.controller.dto.SensorReadingRequest;
import be.ucll.exception.NotFoundException;
import be.ucll.model.SensorReading;
import be.ucll.service.SensorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final SensorService sensorService;

    public DataController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping
    public ResponseEntity<SensorReading> receiveData(@Valid @RequestBody SensorReadingRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorService.save(body));
    }

    @GetMapping
    public ResponseEntity<List<SensorReading>> getAllData() {
        return ResponseEntity.ok(sensorService.getAll());
    }

    @GetMapping("/latest")
    public ResponseEntity<SensorReading> getLatest() {
        return ResponseEntity.ok(orNotFound(sensorService.getLatest(), "No sensor readings yet"));
    }

    @GetMapping("/car/{carId}")
    public ResponseEntity<List<SensorReading>> getByCarId(@PathVariable Integer carId) {
        return ResponseEntity.ok(sensorService.getByCarId(carId));
    }

    @GetMapping("/car/{carId}/latest")
    public ResponseEntity<SensorReading> getLatestByCarId(@PathVariable Integer carId) {
        return ResponseEntity.ok(orNotFound(sensorService.getLatestByCarId(carId), "No sensor readings for car " + carId));
    }

    @GetMapping("/event/{event}")
    public ResponseEntity<List<SensorReading>> getByEvent(@PathVariable String event) {
        return ResponseEntity.ok(sensorService.getByEvent(event));
    }

    @GetMapping("/car/{carId}/event/{event}")
    public ResponseEntity<List<SensorReading>> getByCarIdAndEvent(@PathVariable Integer carId, @PathVariable String event) {
        return ResponseEntity.ok(sensorService.getByCarIdAndEvent(carId, event));
    }

    @GetMapping("/hits")
    public ResponseEntity<List<SensorReading>> getHits() {
        return ResponseEntity.ok(sensorService.getHits());
    }

    @GetMapping("/hits/heavy")
    public ResponseEntity<List<SensorReading>> getHeavyHits() {
        return ResponseEntity.ok(sensorService.getHeavyHits());
    }

    private static SensorReading orNotFound(SensorReading reading, String message) {
        if (reading == null) {
            throw new NotFoundException(message);
        }
        return reading;
    }
}
