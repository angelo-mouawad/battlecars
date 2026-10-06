package be.ucll.controller;

import be.ucll.controller.dto.SensorReadingRequest;
import be.ucll.model.SensorReading;
import be.ucll.service.SensorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ DataController.class, StatusController.class })
@AutoConfigureMockMvc(addFilters = false)
public class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SensorService sensorService;

    // -------- POST /api/data --------

    @Test
    void postData_validReading_returns201() throws Exception {
        SensorReading saved = new SensorReading(1, "move", "{\"isMoving\":true,\"speed\":\"fast\"}");
        saved.setTimestamp(LocalDateTime.of(2026, 5, 11, 10, 0, 0));

        when(sensorService.save(any(SensorReadingRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carId", 1, "event", "move", "data", Map.of("isMoving", true, "speed", "fast")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carId").value(1))
                .andExpect(jsonPath("$.event").value("move"));
    }

    @Test
    void postData_missingCarId_returns400() throws Exception {
        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("carId must not be null"));
    }

    @Test
    void postData_unknownCar_returns400() throws Exception {
        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carId\": 7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postData_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void postData_hitEvent_savesCorrectly() throws Exception {
        SensorReading saved = new SensorReading(2, "hit", "{\"hit\":true,\"hitStrength\":\"heavy\"}");

        when(sensorService.save(any(SensorReadingRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carId", 2, "data", Map.of("hit", true, "hitStrength", "heavy")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carId").value(2))
                .andExpect(jsonPath("$.event").value("hit"));
    }

    // -------- GET /api/data --------

    @Test
    void getAllData_returnsListOfReadings() throws Exception {
        SensorReading r1 = new SensorReading(1, "move", "{\"isMoving\":true}");
        SensorReading r2 = new SensorReading(2, "stop", "{\"isMoving\":false}");

        when(sensorService.getAll()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].carId").value(1))
                .andExpect(jsonPath("$[1].carId").value(2));
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
        SensorReading latest = new SensorReading(1, "speed", "{\"speed\":\"fast\"}");
        latest.setTimestamp(LocalDateTime.of(2026, 5, 11, 14, 0, 0));

        when(sensorService.getLatest()).thenReturn(latest);

        mockMvc.perform(get("/api/data/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carId").value(1))
                .andExpect(jsonPath("$.event").value("speed"));
    }

    @Test
    void getLatest_noData_returns404() throws Exception {
        when(sensorService.getLatest()).thenReturn(null);

        mockMvc.perform(get("/api/data/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No sensor readings yet"));
    }

    // -------- GET /api/data/car/{carId} --------

    @Test
    void getByCarId_returnsCar1Readings() throws Exception {
        SensorReading r1 = new SensorReading(1, "move", "{\"isMoving\":true}");

        when(sensorService.getByCarId(1)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/data/car/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].carId").value(1));
    }

    // -------- GET /api/data/hits --------

    @Test
    void getHits_returnsHitReadings() throws Exception {
        SensorReading hit = new SensorReading(1, "hit", "{\"hit\":true,\"hitStrength\":\"light\"}");

        when(sensorService.getHits()).thenReturn(List.of(hit));

        mockMvc.perform(get("/api/data/hits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].event").value("hit"));
    }

    // -------- GET /status --------

    @Test
    void statusEndpoint_returns200WithOk() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}