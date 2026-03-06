package com.mysawit.mysawit_pembayaran.service;

import com.mysawit.mysawit_pembayaran.dto.response.PaymentTransactionResponse;
import com.mysawit.mysawit_pembayaran.dto.response.WalletResponse;
import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletResponse getWalletByUserId(String userId);
    WalletResponse topUpWallet(String userId, BigDecimal amount);
    List<PaymentTransactionResponse> getTransactions(String userId);
}