package be.ucll.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

public class SensorReadingTest {

    @Test
    void constructor_setsAllFields() {
        SensorReading reading = new SensorReading("esp32-team1", 22.4, false);

        assertThat(reading.getDeviceId()).isEqualTo("esp32-team1");
        assertThat(reading.getTemperature()).isEqualTo(22.4);
        assertThat(reading.getIsMoving()).isFalse();
        assertThat(reading.getTimestamp()).isNotNull();
    }

    @Test
    void constructor_setsTimestampAutomatically() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        SensorReading reading = new SensorReading("esp32-team1", 22.4, false);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(reading.getTimestamp()).isBetween(before, after);
    }

    @Test
    void emptyConstructor_fieldsAreNull() {
        SensorReading reading = new SensorReading();

        assertThat(reading.getDeviceId()).isNull();
        assertThat(reading.getTemperature()).isNull();
        assertThat(reading.getIsMoving()).isNull();
    }

    @Test
    void setters_updateFieldsCorrectly() {
        SensorReading reading = new SensorReading();
        reading.setDeviceId("esp32-team1");
        reading.setTemperature(25.0);
        reading.setIsMoving(true);

        assertThat(reading.getDeviceId()).isEqualTo("esp32-team1");
        assertThat(reading.getTemperature()).isEqualTo(25.0);
        assertThat(reading.getIsMoving()).isTrue();
    }

    @Test
    void temperature_canBeNegative() {
        SensorReading reading = new SensorReading("esp32-team1", -10.5, false);
        assertThat(reading.getTemperature()).isEqualTo(-10.5);
    }

    @Test
    void isMoving_canBeTrue() {
        SensorReading reading = new SensorReading("esp32-team1", 22.4, true);
        assertThat(reading.getIsMoving()).isTrue();
    }
}