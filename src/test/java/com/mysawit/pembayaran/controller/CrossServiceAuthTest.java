package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.service.PayrollService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayrollController.class)
class CrossServiceAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayrollService payrollService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPayroll_withoutXUserId_shouldReturn401() throws Exception {
        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(UUID.randomUUID());
        request.setUserRole(UserRole.BURUH);
        request.setKilogram(100.0);

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayroll_withXUserId_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setKilogram(100.0);

        com.mysawit.pembayaran.dto.response.PayrollResponse response =
                com.mysawit.pembayaran.dto.response.PayrollResponse.builder()
                        .id(UUID.randomUUID()).userId(userId)
                        .userRole(UserRole.BURUH).amount(450000.0).kilogram(100.0)
                        .status(com.mysawit.pembayaran.model.enums.PayrollStatus.PENDING)
                        .description("test").createdAt(java.time.LocalDateTime.now())
                        .updatedAt(java.time.LocalDateTime.now()).build();

        org.mockito.Mockito.when(payrollService.createPayroll(org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
