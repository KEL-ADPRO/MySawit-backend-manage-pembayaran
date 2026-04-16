package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pembayaran/wallet/topup")
@RequiredArgsConstructor
public class PaymentGatewayController {

    private final WalletService walletService;

    @PostMapping("/callback")
    public ResponseEntity<Void> handleXenditCallback(@RequestBody Map<String, Object> payload) {
        walletService.handleTopUpCallback(payload);
        return ResponseEntity.ok().build();
    }
}
