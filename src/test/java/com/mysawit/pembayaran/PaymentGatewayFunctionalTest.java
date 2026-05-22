package com.mysawit.pembayaran;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.pembayaran.client.XenditClient;
import com.mysawit.pembayaran.repository.TopUpTransactionRepository;
import com.mysawit.pembayaran.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentGatewayFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TopUpTransactionRepository topUpTransactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private XenditClient xenditClient;

    @BeforeEach
    void setUp() {
        topUpTransactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void topUpSync_shouldMarkTransactionSuccessAndCreditWallet() throws Exception {
        UUID adminId = UUID.randomUUID();

        when(xenditClient.createInvoice(anyString(), any(BigDecimal.class), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String externalId = invocation.getArgument(0);
                    return Map.of(
                            "id", "xendit-" + externalId,
                            "external_id", externalId,
                            "invoice_url", "https://mock-payment.xendit.co/pay/" + externalId,
                            "status", "PENDING");
                });

        MvcResult createdTopUp = mockMvc.perform(post("/api/pembayaran/wallet/topup")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountRupiah": 100000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amountSawitDollar").value(10.0))
                .andReturn();

        JsonNode topUp = objectMapper.readTree(createdTopUp.getResponse().getContentAsString());
        String topUpId = topUp.get("id").asText();
        String externalId = topUp.get("paymentGatewayRef").asText();

        when(xenditClient.getInvoiceByExternalId(externalId))
                .thenReturn(Map.of("external_id", externalId, "status", "PAID"));

        mockMvc.perform(post("/api/pembayaran/wallet/topup/{id}/sync", topUpId)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/api/pembayaran/wallet/me")
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10.0));

        mockMvc.perform(get("/api/pembayaran/wallet/topup/{id}", topUpId)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(xenditClient).createInvoice(anyString(), eq(new BigDecimal("100000.00")), anyString(), anyString(), anyString());
        verify(xenditClient).getInvoiceByExternalId(externalId);
    }
}
