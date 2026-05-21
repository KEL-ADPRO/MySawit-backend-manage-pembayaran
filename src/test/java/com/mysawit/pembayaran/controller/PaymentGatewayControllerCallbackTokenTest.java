package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.security.HeaderAuthenticationFilter;
import com.mysawit.pembayaran.security.SecurityConfig;
import com.mysawit.pembayaran.service.PaymentGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentGatewayController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
@TestPropertySource(properties = "xendit.callback-token=secret-token")
class PaymentGatewayControllerCallbackTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentGatewayService paymentGatewayService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void handleCallback_validToken_shouldReturn200() throws Exception {
        Map<String, Object> payload = Map.of("external_id", "ext-ref-123", "status", "PAID");

        mockMvc.perform(post("/api/pembayaran/wallet/topup/callback")
                        .header("x-callback-token", "secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentGatewayService).handleCallback(any());
    }

    @Test
    void handleCallback_wrongToken_shouldReturn401() throws Exception {
        Map<String, Object> payload = Map.of("external_id", "ext-ref-123", "status", "PAID");

        mockMvc.perform(post("/api/pembayaran/wallet/topup/callback")
                        .header("x-callback-token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        verify(paymentGatewayService, never()).handleCallback(any());
    }

    @Test
    void handleCallback_missingToken_shouldReturn401() throws Exception {
        Map<String, Object> payload = Map.of("external_id", "ext-ref-123", "status", "PAID");

        mockMvc.perform(post("/api/pembayaran/wallet/topup/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        verify(paymentGatewayService, never()).handleCallback(any());
    }
}
