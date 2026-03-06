package com.mysawit.mysawit_pembayaran.controller;

import com.mysawit.mysawit_pembayaran.dto.request.TopUpWalletRequest;
import com.mysawit.mysawit_pembayaran.dto.response.PaymentTransactionResponse;
import com.mysawit.mysawit_pembayaran.dto.response.WalletResponse;
import com.mysawit.mysawit_pembayaran.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public WalletResponse getWallet(@RequestParam String userId) {
        return walletService.getWalletByUserId(userId);
    }

    @PostMapping("/topup")
    public WalletResponse topUp(@Valid @RequestBody TopUpWalletRequest request) {
        return walletService.topUpWallet(request.getUserId(), request.getAmount());
    }

    @GetMapping("/transactions")
    public List<PaymentTransactionResponse> getTransactions(@RequestParam String userId) {
        return walletService.getTransactions(userId);
    }
}