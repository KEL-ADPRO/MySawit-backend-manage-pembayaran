package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.security.HeaderAuthenticationFilter;
import com.mysawit.pembayaran.security.SecurityConfig;
import com.mysawit.pembayaran.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@Import({SecurityConfig.class, HeaderAuthenticationFilter.class})
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    private WalletResponse buildWalletResponse(UUID userId, String balance) {
        return WalletResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(new BigDecimal(balance))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWallet_shouldCreateForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(walletService.createWallet(userId)).thenReturn(buildWalletResponse(userId, "0.00"));

        mockMvc.perform(post("/api/pembayaran/wallet")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void getWalletByUserId_self_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(walletService.getWalletByUserId(userId)).thenReturn(buildWalletResponse(userId, "250.00"));

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}", userId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.0));
    }

    @Test
    void getWalletByUserId_adminCanViewOtherUser() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(walletService.getWalletByUserId(userId)).thenReturn(buildWalletResponse(userId, "250.00"));

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}", userId)
                        .header("X-User-Id", adminId.toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void getWalletByUserId_otherUser_shouldReturn403() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/pembayaran/wallet/{userId}", otherUserId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isForbidden());

        verify(walletService, never()).getWalletByUserId(any());
    }

    @Test
    void getMyWallet_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(walletService.getWalletByUserId(userId)).thenReturn(buildWalletResponse(userId, "100.00"));

        mockMvc.perform(get("/api/pembayaran/wallet/me")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "BURUH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void getMyWallet_noHeader_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/pembayaran/wallet/me"))
                .andExpect(status().isUnauthorized());
    }
}
