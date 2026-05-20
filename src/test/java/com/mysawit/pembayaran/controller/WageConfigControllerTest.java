package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import com.mysawit.pembayaran.security.HeaderAuthenticationFilter;
import com.mysawit.pembayaran.security.SecurityConfig;
import com.mysawit.pembayaran.service.WageConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WageConfigController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
class WageConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WageConfigService wageConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void getConfig_shouldReturn200() throws Exception {
        WageConfigResponse response = WageConfigResponse.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(bd("1000"))
                .supirTrukWagePerKg(bd("1500"))
                .mandorWagePerKg(bd("2000"))
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigService.getWageConfig()).thenReturn(response);

        mockMvc.perform(get("/api/pembayaran/wage-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buruhWagePerKg").value(1000))
                .andExpect(jsonPath("$.supirTrukWagePerKg").value(1500))
                .andExpect(jsonPath("$.mandorWagePerKg").value(2000));
    }

    @Test
    void updateConfig_admin_shouldReturn200() throws Exception {
        UUID adminId = UUID.randomUUID();
        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(bd("1200"));
        request.setSupirTrukWagePerKg(bd("1700"));
        request.setMandorWagePerKg(bd("2200"));

        WageConfigResponse response = WageConfigResponse.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(bd("1200"))
                .supirTrukWagePerKg(bd("1700"))
                .mandorWagePerKg(bd("2200"))
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigService.updateWageConfig(any())).thenReturn(response);

        mockMvc.perform(put("/api/pembayaran/wage-config")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buruhWagePerKg").value(1200));
    }

    @Test
    void updateConfig_nonAdmin_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(bd("1200"));
        request.setSupirTrukWagePerKg(bd("1700"));
        request.setMandorWagePerKg(bd("2200"));

        mockMvc.perform(put("/api/pembayaran/wage-config")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
