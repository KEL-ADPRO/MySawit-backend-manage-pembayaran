package com.mysawit.pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.model.enums.TopUpStatus;
import com.mysawit.pembayaran.security.HeaderAuthenticationFilter;
import com.mysawit.pembayaran.security.SecurityConfig;
import com.mysawit.pembayaran.service.PaymentGatewayService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentGatewayController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
class PaymentGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentGatewayService paymentGatewayService;

    @Autowired
    private ObjectMapper objectMapper;

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private TopUpResponse buildTopUpResponse(UUID userId) {
        return TopUpResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .amountRupiah(bd("100000.00"))
                .amountSawitDollar(bd("10.00"))
                .paymentGatewayRef("ext-ref-123")
                .paymentUrl("https://mock-payment.xendit.co/pay/ext-ref-123")
                .status(TopUpStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TopUpRequest request(UUID suppliedUserId, String amountRupiah) {
        TopUpRequest request = new TopUpRequest();
        request.setUserId(suppliedUserId);
        request.setAmountRupiah(bd(amountRupiah));
        return request;
    }

    @Test
    void initiateTopUp_admin_shouldReturn201ForAuthenticatedAdmin() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID suppliedUserId = UUID.randomUUID();
        TopUpRequest request = request(suppliedUserId, "100000");

        when(paymentGatewayService.initiateTopUp(eq(adminId), any())).thenReturn(buildTopUpResponse(adminId));

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(adminId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amountSawitDollar").value(10.0));
    }

    @Test
    void initiateTopUp_nonAdmin_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        TopUpRequest request = request(userId, "100000");

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(paymentGatewayService, never()).initiateTopUp(any(), any());
    }

    @Test
    void initiateTopUp_noAuth_shouldReturn401() throws Exception {
        TopUpRequest request = request(UUID.randomUUID(), "100000");

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void initiateTopUp_invalidAmount_shouldReturn400() throws Exception {
        UUID adminId = UUID.randomUUID();
        TopUpRequest request = request(adminId, "15000");

        when(paymentGatewayService.initiateTopUp(eq(adminId), any()))
                .thenThrow(new IllegalArgumentException("amountRupiah must be a positive multiple of 10000"));

        mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .header("X-User-Id", adminId.toString())
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

    @Test
    void getTopUps_authenticated_shouldReturnUserTopUps() throws Exception {
        UUID userId = UUID.randomUUID();
        when(paymentGatewayService.getTopUps(userId)).thenReturn(List.of(buildTopUpResponse(userId)));

        mockMvc.perform(get("/api/pembayaran/wallet/topup")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(paymentGatewayService).getTopUps(userId);
    }

    @Test
    void getTopUp_authenticated_shouldReturnUserTopUp() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID topUpId = UUID.randomUUID();
        TopUpResponse response = buildTopUpResponse(userId);
        response.setId(topUpId);
        when(paymentGatewayService.getTopUp(userId, topUpId)).thenReturn(response);

        mockMvc.perform(get("/api/pembayaran/wallet/topup/{id}", topUpId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topUpId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentGatewayService).getTopUp(userId, topUpId);
    }

    @Test
    void syncTopUp_admin_shouldReturnSyncedTopUp() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID topUpId = UUID.randomUUID();
        TopUpResponse response = buildTopUpResponse(userId);
        response.setId(topUpId);
        response.setStatus(TopUpStatus.SUCCESS);
        when(paymentGatewayService.syncTopUp(userId, topUpId)).thenReturn(response);

        mockMvc.perform(post("/api/pembayaran/wallet/topup/{id}/sync", topUpId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topUpId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentGatewayService).syncTopUp(userId, topUpId);
    }

    @Test
    void syncTopUp_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/pembayaran/wallet/topup/{id}/sync", UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isForbidden());

        verify(paymentGatewayService, never()).syncTopUp(any(), any());
    }
}
