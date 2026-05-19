package be.ucll.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

public class SensorReadingTest {

    @Test
    void constructor_setsAllFields() {
        SensorReading reading = new SensorReading(1, "move", "{\"speed\":\"fast\"}", 10, true,22);

        assertThat(reading.getCarId()).isEqualTo(1);
        assertThat(reading.getEvent()).isEqualTo("move");
        assertThat(reading.getData()).isEqualTo("{\"speed\":\"fast\"}");
        assertThat(reading.getDeviceId()).isEqualTo(10);
        assertThat(reading.getIsMoving()).isTrue();
        assertThat(reading.getTemperature()).isEqualTo(22);
        assertThat(reading.getTimestamp()).isNotNull();
    }

    @Test
    void constructor_setsTimestampAutomatically() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        SensorReading reading = new SensorReading(1, "hit", "{\"hit\":true}", null, null, null);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(reading.getTimestamp()).isBetween(before, after);
    }

    @Test
    void emptyConstructor_fieldsAreNull() {
        SensorReading reading = new SensorReading();

        assertThat(reading.getCarId()).isNull();
        assertThat(reading.getEvent()).isNull();
        assertThat(reading.getData()).isNull();
        assertThat(reading.getDeviceId()).isNull();
        assertThat(reading.getIsMoving()).isNull();
        assertThat(reading.getTemperature()).isNull();
    }

    @Test
    void setters_updateFieldsCorrectly() {
        SensorReading reading = new SensorReading();
        reading.setCarId(2);
        reading.setEvent("stop");
        reading.setData("{\"speed\":\"stopped\"}");
        reading.setDeviceId(5);
        reading.setIsMoving(false);
        reading.setTemperature(18);

        assertThat(reading.getCarId()).isEqualTo(2);
        assertThat(reading.getEvent()).isEqualTo("stop");
        assertThat(reading.getData()).contains("stopped");
        assertThat(reading.getDeviceId()).isEqualTo(5);
        assertThat(reading.getIsMoving()).isFalse();
        assertThat(reading.getTemperature()).isEqualTo(18);
    }

    @Test
    void constructor_car2HitEvent() {
        SensorReading reading = new SensorReading(2, "hit", "{\"hit\":true,\"hitStrength\":\"heavy\"}", 2, true,25);

        assertThat(reading.getCarId()).isEqualTo(2);
        assertThat(reading.getEvent()).isEqualTo("hit");
        assertThat(reading.getData()).contains("heavy");
        assertThat(reading.getDeviceId()).isEqualTo(2);
    }

    @Test
    void constructor_speedEvent() {
        SensorReading reading = new SensorReading(1, "speed", "{\"speed\":\"fast\",\"hit\":false}", 1, true,21);

        assertThat(reading.getCarId()).isEqualTo(1);
        assertThat(reading.getEvent()).isEqualTo("speed");
        assertThat(reading.getData()).contains("fast");
        assertThat(reading.getIsMoving()).isTrue();
    }

    @Test
    void constructor_nullableFields_allowNull() {
        SensorReading reading = new SensorReading(1, "move", "{\"speed\":\"slow\"}", null, null, null);

        assertThat(reading.getDeviceId()).isNull();
        assertThat(reading.getIsMoving()).isNull();
        assertThat(reading.getTemperature()).isNull();
    }
}
