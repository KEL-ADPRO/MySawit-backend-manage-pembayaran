package com.mysawit.mysawit_pembayaran.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysawit.mysawit_pembayaran.dto.request.TopUpWalletRequest;
import com.mysawit.mysawit_pembayaran.dto.response.WalletResponse;
import com.mysawit.mysawit_pembayaran.service.WalletService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void topUp_success() throws Exception {
        TopUpWalletRequest request = new TopUpWalletRequest();
        request.setUserId("ADMIN");
        request.setAmount(new BigDecimal("100"));

        WalletResponse response = WalletResponse.builder()
                .id("wallet-1")
                .userId("ADMIN")
                .balance(new BigDecimal("200"))
                .build();

        when(walletService.topUpWallet("ADMIN", new BigDecimal("100"))).thenReturn(response);

        mockMvc.perform(post("/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("ADMIN"))
                .andExpect(jsonPath("$.balance").value(200));
    }
}