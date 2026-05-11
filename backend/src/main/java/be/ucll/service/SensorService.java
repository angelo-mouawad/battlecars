package be.ucll.service;

import java.util.List;

import org.springframework.stereotype.Service;

import be.ucll.model.SensorReading;
import be.ucll.repository.SensorReadingRepository;

@Service
public class SensorService {

    private final SensorReadingRepository repository;

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
}