package be.ucll.service;

import be.ucll.model.SensorReading;
import be.ucll.repository.SensorReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SensorServiceTest {

    @Mock
    private SensorReadingRepository repository;

    @InjectMocks
    private SensorService sensorService;

    private SensorReading reading;

    @BeforeEach
    void setUp() {
        reading = new SensorReading("esp32-team1", 22.4, false);
        reading.setTimestamp(LocalDateTime.of(2026, 5, 11, 10, 0, 0));
    }

    // -------- save() --------

    @Test
    void save_validReading_returnsSavedReading() {
        when(repository.save(any(SensorReading.class))).thenReturn(reading);

        SensorReading result = sensorService.save(reading);

        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo("esp32-team1");
        assertThat(result.getTemperature()).isEqualTo(22.4);
        assertThat(result.getIsMoving()).isFalse();
    }

    @Test
    void save_callsRepositoryOnce() {
        when(repository.save(any(SensorReading.class))).thenReturn(reading);

        sensorService.save(reading);

        verify(repository, times(1)).save(reading);
    }

    @Test
    void save_readingWithMovingTrue_savedCorrectly() {
        SensorReading movingReading = new SensorReading("esp32-team1", 22.4, true);
        when(repository.save(any(SensorReading.class))).thenReturn(movingReading);

        SensorReading result = sensorService.save(movingReading);

        assertThat(result.getIsMoving()).isTrue();
    }

    // -------- getAll() --------

    @Test
    void getAll_returnsAllReadings() {
        SensorReading r2 = new SensorReading("esp32-team1", 23.0, true);
        when(repository.findAll()).thenReturn(List.of(reading, r2));

        List<SensorReading> result = sensorService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTemperature()).isEqualTo(22.4);
        assertThat(result.get(1).getTemperature()).isEqualTo(23.0);
    }

    @Test
    void getAll_emptyDatabase_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<SensorReading> result = sensorService.getAll();

        assertThat(result).isEmpty();
    }

    @Test
    void getAll_callsRepositoryOnce() {
        when(repository.findAll()).thenReturn(List.of());

        sensorService.getAll();

        verify(repository, times(1)).findAll();
    }

    // -------- getLatest() --------

    @Test
    void getLatest_returnsLatestReading() {
        when(repository.findTopByOrderByTimestampDesc()).thenReturn(reading);

        SensorReading result = sensorService.getLatest();

        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo("esp32-team1");
    }

    @Test
    void getLatest_noData_returnsNull() {
        when(repository.findTopByOrderByTimestampDesc()).thenReturn(null);

        SensorReading result = sensorService.getLatest();

        assertThat(result).isNull();
    }

    @Test
    void getLatest_callsRepositoryOnce() {
        when(repository.findTopByOrderByTimestampDesc()).thenReturn(reading);

        sensorService.getLatest();

        verify(repository, times(1)).findTopByOrderByTimestampDesc();
    }
}