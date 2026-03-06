package com.mysawit.mysawit_pembayaran.controller;

import com.mysawit.mysawit_pembayaran.dto.response.PayrollResponse;
import com.mysawit.mysawit_pembayaran.exception.GlobalExceptionHandler;
import com.mysawit.mysawit_pembayaran.exception.PayrollNotFoundException;
import com.mysawit.mysawit_pembayaran.service.PayrollService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayrollController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayrollService payrollService;

    @Test
    void getPayrolls_success() throws Exception {
        PayrollResponse response = PayrollResponse.builder()
                .id("payroll-1")
                .recipientUserId("user-1")
                .recipientRole("BURUH")
                .amount(new BigDecimal("100"))
                .description("Payroll example")
                .build();

        when(payrollService.getPayrolls("user-1", null)).thenReturn(List.of(response));

        mockMvc.perform(get("/payrolls")
                        .param("userId", "user-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("payroll-1"));
    }

    @Test
    void getPayrollById_notFound() throws Exception {
        when(payrollService.getPayrollById("missing"))
                .thenThrow(new PayrollNotFoundException("missing"));

        mockMvc.perform(get("/payrolls/missing"))
                .andExpect(status().isNotFound());
    }
}