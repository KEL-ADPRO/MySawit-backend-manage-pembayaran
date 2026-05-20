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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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
    void showMockPaymentPage_shouldRenderLocalPaymentActions() throws Exception {
        mockMvc.perform(get("/api/pembayaran/wallet/topup/mock-pay/{externalId}", "ext-ref-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Mock Xendit Payment")))
                .andExpect(content().string(containsString("/api/pembayaran/wallet/topup/mock-pay/ext-ref-123/paid")))
                .andExpect(content().string(containsString("/api/pembayaran/wallet/topup/mock-pay/ext-ref-123/failed")));
    }

    @Test
    void markMockPaymentPaid_shouldTriggerPaidCallback() throws Exception {
        mockMvc.perform(post("/api/pembayaran/wallet/topup/mock-pay/{externalId}/paid", "ext-ref-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payment SUCCESS")));

        verify(paymentGatewayService).handleCallback(argThat(payload ->
                "ext-ref-123".equals(payload.get("external_id"))
                        && "PAID".equals(payload.get("status"))));
    }

    @Test
    void markMockPaymentFailed_shouldTriggerFailedCallback() throws Exception {
        mockMvc.perform(post("/api/pembayaran/wallet/topup/mock-pay/{externalId}/failed", "ext-ref-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payment FAILED")));

        verify(paymentGatewayService).handleCallback(argThat(payload ->
                "ext-ref-123".equals(payload.get("external_id"))
                        && "EXPIRED".equals(payload.get("status"))));
    }
}
