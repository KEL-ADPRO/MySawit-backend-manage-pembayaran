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
    void getPayrolls_shouldReturn200() throws Exception {
        when(payrollService.getPayrolls(any(), any(), any(), any()))
                .thenReturn(List.of(buildResponse(UUID.randomUUID(), PayrollStatus.PENDING)));

        mockMvc.perform(get("/api/pembayaran/payroll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
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
        when(payrollService.approvePayroll(id)).thenReturn(buildResponse(id, PayrollStatus.ACCEPTED));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void approvePayroll_insufficientBalance_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        when(payrollService.approvePayroll(id))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Role", "ADMIN"))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approvePayroll_nonAdmin_shouldReturn403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/pembayaran/payroll/{id}/approve", id)
                        .header("X-User-Role", "WORKER"))
                .andExpect(status().isForbidden());
    }
}
