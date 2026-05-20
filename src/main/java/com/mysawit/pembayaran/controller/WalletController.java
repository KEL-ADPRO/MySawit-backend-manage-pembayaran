package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.security.AuthenticatedUser;
import com.mysawit.pembayaran.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pembayaran/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID userId) {
        if (!currentUser.isAdmin() && !userId.equals(currentUser.userId())) {
            throw new AccessDeniedException("Users can only view their own wallet");
        }
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getOwnWallet(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(walletService.getWalletByUserId(currentUser.userId()));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(201).body(walletService.createWallet(currentUser.userId()));
    }
}
