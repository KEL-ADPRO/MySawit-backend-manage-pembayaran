package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.dto.response.WalletResponse;

import java.util.Map;
import java.util.UUID;

public interface WalletService {

    WalletResponse getWalletByUserId(UUID userId);

    WalletResponse createWallet(UUID userId);

    TopUpResponse initiateTopUp(TopUpRequest request);

    void handleTopUpCallback(Map<String, Object> payload);

    WalletResponse addBalance(UUID userId, double amount);

    WalletResponse deductBalance(UUID userId, double amount);
}
