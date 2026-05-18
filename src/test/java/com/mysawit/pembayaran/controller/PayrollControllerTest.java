package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.service.PayrollService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayrollController.class)
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayrollService payrollService;

    @Autowired
    private ObjectMapper objectMapper;

    private PayrollResponse buildResponse(UUID id, PayrollStatus status) {
        return PayrollResponse.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .userRole(UserRole.BURUH)
                .amount(450000.0)
                .kilogram(100.0)
                .status(status)
                .description("test")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createPayroll_shouldReturn201() throws Exception {
        UUID payrollId = UUID.randomUUID();
        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(UUID.randomUUID());
        request.setUserRole(UserRole.BURUH);
        request.setKilogram(100.0);

        when(payrollService.createPayroll(any())).thenReturn(buildResponse(payrollId, PayrollStatus.PENDING));

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getPayrolls_adminCanListSelectedUserWithStatusAndDate_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 1, 31, 23, 59);
        when(payrollService.getPayrolls(eq(PayrollStatus.PENDING), eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(List.of(buildResponse(UUID.randomUUID(), PayrollStatus.PENDING)));

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Role", "ADMIN")
                        .param("userId", userId.toString())
                        .param("status", "PENDING")
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-01-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(payrollService).getPayrolls(PayrollStatus.PENDING, userId, startDate, endDate);
    }

    @Test
    void getPayrolls_buruhOmittingUserId_shouldDefaultToOwnPayroll() throws Exception {
        UUID requesterId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2025, 2, 1, 0, 0);
        when(payrollService.getPayrolls(eq(PayrollStatus.ACCEPTED), eq(requesterId), eq(startDate), any()))
                .thenReturn(List.of(buildResponse(UUID.randomUUID(), PayrollStatus.ACCEPTED)));

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Role", "BURUH")
                        .header("X-User-Id", requesterId.toString())
                        .param("status", "ACCEPTED")
                        .param("startDate", "2025-02-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(payrollService).getPayrolls(PayrollStatus.ACCEPTED, requesterId, startDate, null);
    }

    @Test
    void getPayrolls_supirTrukCanListOwnPayroll() throws Exception {
        UUID requesterId = UUID.randomUUID();
        LocalDateTime endDate = LocalDateTime.of(2025, 2, 28, 23, 59);
        when(payrollService.getPayrolls(eq(PayrollStatus.ACCEPTED), eq(requesterId), any(), eq(endDate)))
                .thenReturn(List.of(buildResponse(UUID.randomUUID(), PayrollStatus.ACCEPTED)));

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Role", "SUPIR_TRUK")
                        .header("X-User-Id", requesterId.toString())
                        .param("userId", requesterId.toString())
                        .param("status", "ACCEPTED")
                        .param("endDate", "2025-02-28T23:59:00"))
                .andExpect(status().isOk());

        verify(payrollService).getPayrolls(PayrollStatus.ACCEPTED, requesterId, null, endDate);
    }

    @Test
    void getPayrolls_mandorCanListOwnPayroll() throws Exception {
        UUID requesterId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.of(2025, 3, 1, 0, 0);
        when(payrollService.getPayrolls(eq(PayrollStatus.REJECTED), eq(requesterId), eq(startDate), any()))
                .thenReturn(List.of(buildResponse(UUID.randomUUID(), PayrollStatus.REJECTED)));

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Role", "MANDOR")
                        .header("X-User-Id", requesterId.toString())
                        .param("userId", requesterId.toString())
                        .param("status", "REJECTED")
                        .param("startDate", "2025-03-01T00:00:00"))
                .andExpect(status().isOk());

        verify(payrollService).getPayrolls(PayrollStatus.REJECTED, requesterId, startDate, null);
    }

    @Test
    void getPayrolls_nonAdminCannotListAnotherUser_shouldReturn403() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/pembayaran/payroll")
                        .header("X-User-Role", "BURUH")
                        .header("X-User-Id", requesterId.toString())
                        .param("userId", otherUserId.toString()))
                .andExpect(status().isForbidden());

        verify(payrollService, never()).getPayrolls(any(), any(), any(), any());
    }

    @Test
    void getPayrollById_found_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(payrollService.getPayrollById(id)).thenReturn(buildResponse(id, PayrollStatus.PENDING));

        mockMvc.perform(get("/api/pembayaran/payroll/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(450000.0));
    }

    @Test
    void getPayrollById_notFound_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(payrollService.getPayrollById(id)).thenThrow(new PayrollNotFoundException(id));

        mockMvc.perform(get("/api/pembayaran/payroll/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void approvePayroll_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(payrollService.approvePayroll(id, adminId)).thenReturn(buildResponse(id, PayrollStatus.ACCEPTED));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void approvePayroll_insufficientBalance_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(payrollService.approvePayroll(id, adminId))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", adminId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectPayroll_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Quality not met");

        when(payrollService.rejectPayroll(eq(id), any()))
                .thenReturn(buildResponse(id, PayrollStatus.REJECTED));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/reject", id)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectPayroll_blankReason_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("");

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/reject", id)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectPayroll_nullReason_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/reject", id)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approvePayroll_nonAdmin_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Role", "WORKER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvePayroll_adminWithoutUserId_shouldReturn401() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectPayroll_nonAdmin_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();
        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Quality not met");

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/reject", id)
                        .header("X-User-Role", "BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(payrollService, never()).rejectPayroll(any(), any());
    }

    @Test
    void getPayrollById_invalidUUID_shouldReturn400ValidationFailed() throws Exception {
        mockMvc.perform(get("/api/pembayaran/payroll/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.id").exists());
    }

    @Test
    void getPayrolls_invalidStatus_shouldReturn400ValidationFailed() throws Exception {
        mockMvc.perform(get("/api/pembayaran/payroll").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void createPayroll_invalidRole_shouldReturn400ValidationFailed() throws Exception {
        String body = "{\"userId\":\"" + UUID.randomUUID()
                + "\",\"userRole\":\"NOT_A_ROLE\",\"kilogram\":10.0}";

        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.userRole").exists());
    }

    @Test
    void createPayroll_missingFields_shouldReturn400ValidationFailed() throws Exception {
        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.userId").exists())
                .andExpect(jsonPath("$.fieldErrors.userRole").exists());
    }

    @Test
    void createPayroll_malformedJson_shouldReturn400ValidationFailed() throws Exception {
        mockMvc.perform(post("/api/pembayaran/payroll")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.body").exists());
    }
}
