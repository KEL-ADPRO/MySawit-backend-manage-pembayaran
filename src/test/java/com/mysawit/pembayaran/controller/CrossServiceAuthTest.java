package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.security.HeaderAuthenticationFilter;
import com.mysawit.pembayaran.security.SecurityConfig;
import com.mysawit.pembayaran.service.PayrollService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayrollController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
class CrossServiceAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayrollService payrollService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPayroll_withoutTrustedHeaders_shouldReturn401() throws Exception {
        CreatePayrollRequest request = buildRequest(UUID.randomUUID());

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayroll_withOnlyUserIdHeader_shouldReturn401() throws Exception {
        UUID userId = UUID.randomUUID();
        CreatePayrollRequest request = buildRequest(userId);

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayroll_withAdminTrustedHeaders_shouldReturn201() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreatePayrollRequest request = buildRequest(userId);

        when(payrollService.createPayroll(any()))
                .thenReturn(PayrollResponse.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .userRole(UserRole.BURUH)
                        .amount(new BigDecimal("4500.00"))
                        .kilogram(new BigDecimal("100.000"))
                        .status(PayrollStatus.PENDING)
                        .description("test")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private CreatePayrollRequest buildRequest(UUID userId) {
        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setHarvestedKg(new BigDecimal("100"));
        return request;
    }
}
