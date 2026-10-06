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
import static org.junit.jupiter.api.Assertions.*;
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
        reading = new SensorReading(1, "move", "{\"isMoving\":true,\"speed\":\"fast\",\"hit\":false,\"hitStrength\":\"none\",\"orientation\":\"unknown\"}");
        reading.setTimestamp(LocalDateTime.of(2026, 5, 11, 10, 0, 0));
    }

    // -------- save() --------

    @Test
    void save_validReading_returnsSavedReading() {
        when(repository.save(any(SensorReading.class))).thenReturn(reading);

        SensorReading result = sensorService.save(reading);

        assertThat(result).isNotNull();
        assertThat(result.getCarId()).isEqualTo(1);
        assertThat(result.getEvent()).isEqualTo("move");
        assertThat(result.getData()).contains("fast");
    }

    @Test
    void save_callsRepositoryOnce() {
        when(repository.save(any(SensorReading.class))).thenReturn(reading);

        sensorService.save(reading);

        verify(repository, times(1)).save(reading);
    }

    @Test
    void save_car2HitReading_savedCorrectly() {
        SensorReading hitReading = new SensorReading(2, "hit", "{\"isMoving\":true,\"hit\":true,\"hitStrength\":\"heavy\"}");
        when(repository.save(any(SensorReading.class))).thenReturn(hitReading);

        SensorReading result = sensorService.save(hitReading);

        assertThat(result.getCarId()).isEqualTo(2);
        assertThat(result.getEvent()).isEqualTo("hit");
        assertThat(result.getData()).contains("heavy");
    }

    // -------- getAll() --------

    @Test
    void getAll_returnsAllReadings() {
        SensorReading r2 = new SensorReading(2, "stop", "{\"isMoving\":false,\"speed\":\"stopped\"}");
        when(repository.findAll()).thenReturn(List.of(reading, r2));

        List<SensorReading> result = sensorService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCarId()).isEqualTo(1);
        assertThat(result.get(1).getCarId()).isEqualTo(2);
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
        assertThat(result.getCarId()).isEqualTo(1);
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

    // -------- saveFromJson() --------

    @Test
    void saveFromJson_validJson_savesCorrectly() throws Exception {
        String json = "{\"carId\":1,\"event\":\"move\",\"isMoving\":true,\"speed\":\"fast\",\"hit\":false,\"hitStrength\":\"none\",\"orientation\":\"unknown\"}";

        SensorReading mockReading = new SensorReading(1, "move", "{\"isMoving\":true,\"speed\":\"fast\",\"hit\":false,\"hitStrength\":\"none\",\"orientation\":\"unknown\"}");
        when(repository.save(any(SensorReading.class))).thenReturn(mockReading);

        SensorReading result = sensorService.saveFromJson(json);

        assertThat(result.getCarId()).isEqualTo(1);
        assertThat(result.getEvent()).isEqualTo("move");
        assertThat(result.getData()).contains("fast");
    }

    @Test
    void saveFromJson_hitEvent_savesCorrectly() throws Exception {
        String json = "{\"carId\":2,\"event\":\"hit\",\"isMoving\":true,\"speed\":\"unknown\",\"hit\":true,\"hitStrength\":\"heavy\",\"orientation\":\"unknown\"}";

        SensorReading mockReading = new SensorReading(2, "hit", "{\"isMoving\":true,\"speed\":\"unknown\",\"hit\":true,\"hitStrength\":\"heavy\",\"orientation\":\"unknown\"}");
        when(repository.save(any(SensorReading.class))).thenReturn(mockReading);

        SensorReading result = sensorService.saveFromJson(json);

        assertThat(result.getCarId()).isEqualTo(2);
        assertThat(result.getEvent()).isEqualTo("hit");
        assertThat(result.getData()).contains("heavy");
    }

    @Test
    void saveFromJson_missingCarId_isRejectedWithoutSaving() {
        String json = "{\"event\":\"move\",\"isMoving\":true}";

        assertThrows(IllegalArgumentException.class, () -> sensorService.saveFromJson(json));
        verify(repository, never()).save(any(SensorReading.class));
    }

    @Test
    void saveFromJson_invalidJson_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> sensorService.saveFromJson("{not json"));
        assertThrows(IllegalArgumentException.class, () -> sensorService.saveFromJson("[1, 2]"));
        assertThrows(IllegalArgumentException.class, () -> sensorService.saveFromJson(""));
    }

    @Test
    void saveFromJson_hubMessageWithoutEvent_derivesEventFromHitFlag() {
        when(repository.save(any(SensorReading.class))).thenAnswer(i -> i.getArgument(0));

        SensorReading hit = sensorService.saveFromJson("{\"carId\":1,\"hit\":true,\"orientation\":\"upright\"}");
        SensorReading orientation = sensorService.saveFromJson("{\"carId\":2,\"hit\":false,\"orientation\":\"flipped\"}");

        assertThat(hit.getEvent()).isEqualTo("hit");
        assertThat(hit.getCarId()).isEqualTo(1);
        assertThat(orientation.getEvent()).isEqualTo("orientation");
        assertThat(orientation.getData()).contains("flipped").doesNotContain("carId");
    }

    // -------- getByCarId() --------

    @Test
    void getByCarId_returnsCar1Readings() {
        when(repository.findByCarId(1)).thenReturn(List.of(reading));

        List<SensorReading> result = sensorService.getByCarId(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCarId()).isEqualTo(1);
    }

    // -------- getHits() --------

    @Test
    void getHits_returnsHitReadings() {
        SensorReading hit = new SensorReading(1, "hit", "{\"hit\":true,\"hitStrength\":\"light\"}");
        when(repository.findAllHits()).thenReturn(List.of(hit));

        List<SensorReading> result = sensorService.getHits();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEvent()).isEqualTo("hit");
    }
}