package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.model.enums.PayrollKilogramType;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayrollController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayrollService payrollService;

    @Autowired
    private ObjectMapper objectMapper;

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private PayrollResponse buildResponse(UUID id, UUID userId, PayrollStatus status) {
        return PayrollResponse.builder()
                .id(id)
                .userId(userId)
                .userRole(UserRole.BURUH)
                .amount(bd("4500.00"))
                .kilogram(bd("100.000"))
                .harvestedKg(bd("100.000"))
                .kilogramType(PayrollKilogramType.HARVESTED)
                .sourceType(PayrollSourceType.MANUAL_ADMIN)
                .status(status)
                .description("Payroll BURUH dari MANUAL_ADMIN: 100 kg harvested x 50 SawitDollar/kg x 90% = 4500 SawitDollar")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CreatePayrollRequest buildCreateRequest(UUID userId) {
        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setHarvestedKg(bd("100"));
        return request;
    }

    @Test
    void createPayroll_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/pembayaran/payroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(UUID.randomUUID()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayroll_nonAdmin_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(userId))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPayroll_admin_shouldReturn201() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID payrollId = UUID.randomUUID();
        when(payrollService.createPayroll(any())).thenReturn(buildResponse(payrollId, userId, PayrollStatus.PENDING));

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(userId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getPayrolls_adminCanQueryUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(payrollService.getPayrolls(eq(PayrollStatus.PENDING), eq(userId), any(), any()))
                .thenReturn(List.of(buildResponse(UUID.randomUUID(), userId, PayrollStatus.PENDING)));

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .param("userId", userId.toString())
                        .param("status", "PENDING")
                        .param("startDate", "2026-05-01T00:00:00")
                        .param("endDate", "2026-05-20T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(payrollService).getPayrolls(eq(PayrollStatus.PENDING), eq(userId), any(), any());
    }

    @Test
    void getPayrolls_userOnlySeesSelf() throws Exception {
        UUID userId = UUID.randomUUID();
        when(payrollService.getPayrolls(eq(null), eq(userId), eq(null), eq(null)))
                .thenReturn(List.of(buildResponse(UUID.randomUUID(), userId, PayrollStatus.PENDING)));

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(payrollService).getPayrolls(null, userId, null, null);
    }

    @Test
    void getPayrolls_userCannotQueryOtherUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH")
                        .param("userId", otherUserId.toString()))
                .andExpect(status().isForbidden());

        verify(payrollService, never()).getPayrolls(any(), any(), any(), any());
    }

    @Test
    void getPayrollById_userCannotViewOtherUserPayroll() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID payrollId = UUID.randomUUID();
        when(payrollService.getPayrollById(payrollId)).thenReturn(buildResponse(payrollId, otherUserId, PayrollStatus.PENDING));

        mockMvc.perform(get("/api/pembayaran/payroll/{id}", payrollId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPayrollById_notFound_shouldReturn404() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(payrollService.getPayrollById(id)).thenThrow(new PayrollNotFoundException(id));

        mockMvc.perform(get("/api/pembayaran/payroll/{id}", id)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void approvePayroll_admin_shouldReturn200() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(payrollService.approvePayroll(id, adminId)).thenReturn(buildResponse(id, UUID.randomUUID(), PayrollStatus.ACCEPTED));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void approvePayroll_nonAdmin_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvePayroll_insufficientBalance_shouldReturn400() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(payrollService.approvePayroll(id, adminId))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectPayroll_admin_shouldReturn200() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Quality not met");

        when(payrollService.rejectPayroll(eq(id), any()))
                .thenReturn(buildResponse(id, UUID.randomUUID(), PayrollStatus.REJECTED));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/reject", id)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectPayroll_nonAdmin_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Quality not met");

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/reject", id)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
