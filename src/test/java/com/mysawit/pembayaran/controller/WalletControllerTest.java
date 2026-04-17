package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    private WalletResponse buildWalletResponse(UUID userId, double balance) {
        return WalletResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(balance)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWallet_shouldReturn201() throws Exception {
        UUID userId = UUID.randomUUID();
        when(walletService.createWallet(any())).thenReturn(buildWalletResponse(userId, 0.0));

        mockMvc.perform(post("/api/pembayaran/wallet")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void getWalletByUserId_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(walletService.getWalletByUserId(userId)).thenReturn(buildWalletResponse(userId, 250.0));

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.0));
    }

    @Test
    void getMyWallet_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(walletService.getWalletByUserId(userId)).thenReturn(buildWalletResponse(userId, 100.0));

        mockMvc.perform(get("/api/pembayaran/wallet/me")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void getMyWallet_noHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/pembayaran/wallet/me"))
                .andExpect(status().isUnauthorized());
    }
}
