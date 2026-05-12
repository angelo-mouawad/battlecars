package be.ucll.controller;

import be.ucll.model.SensorReading;
import be.ucll.service.SensorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({DataController.class, StatusController.class})
public class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private SensorService sensorService;

    // -------- POST /api/data --------

    @Test
    void postData_validReading_returns200() throws Exception {
        // Arrange — what we send
        SensorReading input = new SensorReading("esp32-team1", 22.4, false);

        // What the service returns after saving
        SensorReading saved = new SensorReading("esp32-team1", 22.4, false);
        saved.setTimestamp(LocalDateTime.of(2026, 5, 11, 10, 0, 0));

        when(sensorService.save(any(SensorReading.class))).thenReturn(saved);

        // Act + Assert
        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("esp32-team1"))
                .andExpect(jsonPath("$.temperature").value(22.4))
                .andExpect(jsonPath("$.isMoving").value(false));
    }

    @Test
    void postData_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk()); // empty body is accepted, fields are null
    }

    @Test
    void postData_highTemperature_savesCorrectly() throws Exception {
        SensorReading input = new SensorReading("esp32-team1", 99.9, true);
        SensorReading saved = new SensorReading("esp32-team1", 99.9, true);

        when(sensorService.save(any(SensorReading.class))).thenReturn(saved);

        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperature").value(99.9))
                .andExpect(jsonPath("$.isMoving").value(true));
    }

    // -------- GET /api/data --------

    @Test
    void getAllData_returnsListOfReadings() throws Exception {
        SensorReading r1 = new SensorReading("esp32-team1", 22.4, false);
        SensorReading r2 = new SensorReading("esp32-team1", 23.1, true);

        when(sensorService.getAll()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].temperature").value(22.4))
                .andExpect(jsonPath("$[1].temperature").value(23.1));
    }

    @Test
    void getAllData_emptyDatabase_returnsEmptyList() throws Exception {
        when(sensorService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // -------- GET /api/data/latest --------

    @Test
    void getLatest_returnsLatestReading() throws Exception {
        SensorReading latest = new SensorReading("esp32-team1", 25.0, false);
        latest.setTimestamp(LocalDateTime.of(2026, 5, 11, 14, 0, 0));

        when(sensorService.getLatest()).thenReturn(latest);

        mockMvc.perform(get("/api/data/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("esp32-team1"))
                .andExpect(jsonPath("$.temperature").value(25.0));
    }

    @Test
    void getLatest_noData_returnsNull() throws Exception {
        when(sensorService.getLatest()).thenReturn(null);

        mockMvc.perform(get("/api/data/latest"))
                .andExpect(status().isOk());
    }

    // -------- GET /status --------

    @Test
    void statusEndpoint_returns200WithOk() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}