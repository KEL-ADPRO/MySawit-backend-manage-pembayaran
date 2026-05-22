package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.security.HeaderAuthenticationFilter;
import com.mysawit.pembayaran.security.SecurityConfig;
import com.mysawit.pembayaran.service.PaymentGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MockPaymentController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
@ActiveProfiles("dev")
class MockPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentGatewayService paymentGatewayService;

    @Test
    void showMockPaymentPage_devProfile_shouldRenderLocalPaymentActions() throws Exception {
        mockMvc.perform(get("/api/pembayaran/wallet/topup/mock-pay/{externalId}", "ext-ref-123"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Mock Xendit Payment")))
                .andExpect(content().string(containsString("/api/pembayaran/wallet/topup/mock-pay/ext-ref-123/paid")))
                .andExpect(content().string(containsString("/api/pembayaran/wallet/topup/mock-pay/ext-ref-123/failed")));
    }

    @Test
    void markMockPaymentPaid_devProfile_shouldTriggerPaidCallback() throws Exception {
        mockMvc.perform(post("/api/pembayaran/wallet/topup/mock-pay/{externalId}/paid", "ext-ref-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payment SUCCESS")));

        verify(paymentGatewayService).handleCallback(argThat(payload ->
                "ext-ref-123".equals(payload.get("external_id"))
                        && "PAID".equals(payload.get("status"))));
    }

    @Test
    void markMockPaymentFailed_devProfile_shouldTriggerFailedCallback() throws Exception {
        mockMvc.perform(post("/api/pembayaran/wallet/topup/mock-pay/{externalId}/failed", "ext-ref-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payment FAILED")));

        verify(paymentGatewayService).handleCallback(argThat(payload ->
                "ext-ref-123".equals(payload.get("external_id"))
                        && "EXPIRED".equals(payload.get("status"))));
    }
}
