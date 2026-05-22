package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.response.WalletResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    WalletResponse getWalletByUserId(UUID userId);

    WalletResponse createWallet(UUID userId);

    WalletResponse addBalance(UUID userId, BigDecimal amount);

    WalletResponse deductBalance(UUID userId, BigDecimal amount);
}
