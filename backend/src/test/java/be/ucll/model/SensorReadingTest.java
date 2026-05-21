package be.ucll.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

public class SensorReadingTest {

    @Test
    void constructor_setsAllFields() {
        SensorReading reading = new SensorReading(1, "move", "{\"isMoving\":true,\"speed\":\"fast\"}");

        assertThat(reading.getCarId()).isEqualTo(1);
        assertThat(reading.getEvent()).isEqualTo("move");
        assertThat(reading.getData()).isEqualTo("{\"isMoving\":true,\"speed\":\"fast\"}");
        assertThat(reading.getTimestamp()).isNotNull();
    }

    @Test
    void constructor_setsTimestampAutomatically() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        SensorReading reading = new SensorReading(1, "hit", "{\"hit\":true}");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(reading.getTimestamp()).isBetween(before, after);
    }

    @Test
    void emptyConstructor_fieldsAreNull() {
        SensorReading reading = new SensorReading();

        assertThat(reading.getCarId()).isNull();
        assertThat(reading.getEvent()).isNull();
        assertThat(reading.getData()).isNull();
    }

    @Test
    void setters_updateFieldsCorrectly() {
        SensorReading reading = new SensorReading();
        reading.setCarId(2);
        reading.setEvent("stop");
        reading.setData("{\"isMoving\":false,\"speed\":\"stopped\"}");

        assertThat(reading.getCarId()).isEqualTo(2);
        assertThat(reading.getEvent()).isEqualTo("stop");
        assertThat(reading.getData()).contains("stopped");
    }

    @Test
    void constructor_car2HitEvent() {
        SensorReading reading = new SensorReading(2, "hit", "{\"hit\":true,\"hitStrength\":\"heavy\"}");

        assertThat(reading.getCarId()).isEqualTo(2);
        assertThat(reading.getEvent()).isEqualTo("hit");
        assertThat(reading.getData()).contains("heavy");
    }

    @Test
    void constructor_speedEvent() {
        SensorReading reading = new SensorReading(1, "speed", "{\"isMoving\":true,\"speed\":\"fast\",\"hit\":false}");

        assertThat(reading.getCarId()).isEqualTo(1);
        assertThat(reading.getEvent()).isEqualTo("speed");
        assertThat(reading.getData()).contains("fast");
    }
}