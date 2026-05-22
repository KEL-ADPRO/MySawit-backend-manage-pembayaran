package com.mysawit.pembayaran.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XenditClientImplTest {

    private XenditClientImpl xenditClient;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        xenditClient = new XenditClientImpl(restTemplate);
        ReflectionTestUtils.setField(xenditClient, "apiKey", "");
        ReflectionTestUtils.setField(xenditClient, "publicBaseUrl", "");
        ReflectionTestUtils.setField(xenditClient, "serverPort", "8085");
    }

    @Test
    void createInvoice_withoutApiKey_shouldReturnLocalMockPaymentUrl() {
        Map<String, Object> invoice = xenditClient.createInvoice("ext-ref-123", new BigDecimal("100000"), "TopUp", "", "");

        assertThat(invoice.get("external_id")).isEqualTo("ext-ref-123");
        assertThat(invoice.get("invoice_url"))
                .isEqualTo("http://localhost:8085/api/pembayaran/wallet/topup/mock-pay/ext-ref-123");
    }

    @Test
    void createInvoice_withPublicBaseUrl_shouldUseConfiguredBaseUrl() {
        ReflectionTestUtils.setField(xenditClient, "publicBaseUrl", "https://api.example.test/");

        Map<String, Object> invoice = xenditClient.createInvoice("ext-ref-456", new BigDecimal("100000"), "TopUp", "", "");

        assertThat(invoice.get("invoice_url"))
                .isEqualTo("https://api.example.test/api/pembayaran/wallet/topup/mock-pay/ext-ref-456");
    }

    @Test
    void getInvoiceByExternalId_withoutApiKey_shouldReturnEmptyMap() {
        Map<String, Object> invoice = xenditClient.getInvoiceByExternalId("ext-ref-123");

        assertThat(invoice).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getInvoiceByExternalId_withApiKey_shouldCallXenditInvoiceListEndpoint() {
        ReflectionTestUtils.setField(xenditClient, "apiKey", "secret-key");
        Map<String, Object> invoicePayload = Map.of(
                "external_id", "ext-ref-123",
                "status", "PAID");
        doReturn(ResponseEntity.ok(List.of(invoicePayload))).when(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class));

        Map<String, Object> invoice = xenditClient.getInvoiceByExternalId("ext-ref-123");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class));
        assertThat(urlCaptor.getValue())
                .isEqualTo("https://api.xendit.co/v2/invoices?external_id=ext-ref-123");
        assertThat(invoice.get("status")).isEqualTo("PAID");
    }
}
