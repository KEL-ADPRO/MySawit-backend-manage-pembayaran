package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import com.mysawit.pembayaran.service.PaymentGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentGatewayController.class)
class PaymentGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentGatewayService paymentGatewayService;

    @Autowired
    private ObjectMapper objectMapper;

    private TopUpResponse buildTopUpResponse(UUID userId) {
        return TopUpResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amountRupiah(100000.0)
                .amountSawitDollar(10.0)
                .paymentGatewayRef("ext-ref-123")
                .paymentUrl("https://mock-payment.xendit.co/pay/ext-ref-123")
                .status(TopUpStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void initiateTopUp_admin_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmountRupiah(100000.0);

        when(paymentGatewayService.initiateTopUp(any())).thenReturn(buildTopUpResponse(userId));

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amountSawitDollar").value(10.0));
    }

    @Test
    void initiateTopUp_nonAdmin_shouldReturn403() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(UUID.randomUUID());
        request.setAmountRupiah(100000.0);

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .header("X-User-Role", "WORKER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(paymentGatewayService, never()).initiateTopUp(any());
    }

    @Test
    void initiateTopUp_noRole_shouldReturn403() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(UUID.randomUUID());
        request.setAmountRupiah(100000.0);

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void initiateTopUp_invalidAmount_shouldReturn400() throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(UUID.randomUUID());
        request.setAmountRupiah(15000.0);

        when(paymentGatewayService.initiateTopUp(any()))
                .thenThrow(new IllegalArgumentException("amountRupiah must be a positive multiple of 10000"));

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handleCallback_shouldReturn200() throws Exception {
        Map<String, Object> payload = Map.of("external_id", "ext-ref-123", "status", "PAID");
        doNothing().when(paymentGatewayService).handleCallback(any());

        mockMvc.perform(post("/api/pembayaran/wallet/topup/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentGatewayService).handleCallback(any());
    }
}
