package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import com.mysawit.pembayaran.service.WageConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WageConfigController.class)
class WageConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WageConfigService wageConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getConfig_shouldReturn200() throws Exception {
        WageConfigResponse response = WageConfigResponse.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(1000.0)
                .supirTrukWagePerKg(1500.0)
                .mandorWagePerKg(2000.0)
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigService.getWageConfig()).thenReturn(response);

        mockMvc.perform(get("/api/pembayaran/wage-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buruhWagePerKg").value(1000.0))
                .andExpect(jsonPath("$.supirTrukWagePerKg").value(1500.0))
                .andExpect(jsonPath("$.mandorWagePerKg").value(2000.0));
    }

    @Test
    void updateConfig_shouldReturn200() throws Exception {
        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(1200.0);
        request.setSupirTrukWagePerKg(1700.0);
        request.setMandorWagePerKg(2200.0);

        WageConfigResponse response = WageConfigResponse.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(1200.0)
                .supirTrukWagePerKg(1700.0)
                .mandorWagePerKg(2200.0)
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigService.updateWageConfig(any())).thenReturn(response);

        mockMvc.perform(put("/api/pembayaran/wage-config")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buruhWagePerKg").value(1200.0));
    }

    @Test
    void updateConfig_nonAdmin_shouldReturn403() throws Exception {
        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(1200.0);
        request.setSupirTrukWagePerKg(1700.0);
        request.setMandorWagePerKg(2200.0);

        mockMvc.perform(put("/api/pembayaran/wage-config")
                        .header("X-User-Role", "WORKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
