package com.mysawit.pembayaran.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class XenditClientImplTest {

    private XenditClientImpl xenditClient;

    @BeforeEach
    void setUp() {
        xenditClient = new XenditClientImpl(mock(RestTemplate.class));
        ReflectionTestUtils.setField(xenditClient, "apiKey", "");
        ReflectionTestUtils.setField(xenditClient, "publicBaseUrl", "");
        ReflectionTestUtils.setField(xenditClient, "serverPort", "8085");
    }

    @Test
    void createInvoice_withoutApiKey_shouldReturnLocalMockPaymentUrl() {
        Map<String, Object> invoice = xenditClient.createInvoice("ext-ref-123", 100000.0, "TopUp", "", "");

        assertThat(invoice.get("external_id")).isEqualTo("ext-ref-123");
        assertThat(invoice.get("invoice_url"))
                .isEqualTo("http://localhost:8085/api/pembayaran/wallet/topup/mock-pay/ext-ref-123");
    }

    @Test
    void createInvoice_withPublicBaseUrl_shouldUseConfiguredBaseUrl() {
        ReflectionTestUtils.setField(xenditClient, "publicBaseUrl", "https://api.example.test/");

        Map<String, Object> invoice = xenditClient.createInvoice("ext-ref-456", 100000.0, "TopUp", "", "");

        assertThat(invoice.get("invoice_url"))
                .isEqualTo("https://api.example.test/api/pembayaran/wallet/topup/mock-pay/ext-ref-456");
    }
}
