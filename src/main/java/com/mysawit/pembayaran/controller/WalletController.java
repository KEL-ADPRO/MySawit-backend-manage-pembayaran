package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pembayaran/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getOwnWallet(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createWallet(userId));
    }

    @PostMapping("/topup")
    public ResponseEntity<TopUpResponse> initiateTopUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.initiateTopUp(request));
    }
}
