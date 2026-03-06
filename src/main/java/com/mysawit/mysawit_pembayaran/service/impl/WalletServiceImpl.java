package com.mysawit.mysawit_pembayaran.service.impl;

import com.mysawit.mysawit_pembayaran.dto.response.PaymentTransactionResponse;
import com.mysawit.mysawit_pembayaran.dto.response.WalletResponse;
import com.mysawit.mysawit_pembayaran.exception.WalletNotFoundException;
import com.mysawit.mysawit_pembayaran.model.PaymentTransaction;
import com.mysawit.mysawit_pembayaran.model.PaymentTransactionStatus;
import com.mysawit.mysawit_pembayaran.model.Wallet;
import com.mysawit.mysawit_pembayaran.repository.PaymentTransactionRepository;
import com.mysawit.mysawit_pembayaran.repository.WalletRepository;
import com.mysawit.mysawit_pembayaran.service.WalletService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public WalletResponse getWalletByUserId(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        return toWalletResponse(wallet);
    }

    @Override
    public WalletResponse topUpWallet(String userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top up amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setWalletId(savedWallet.getId());
        transaction.setAmount(amount);
        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transaction.setExternalReference("DUMMY-" + System.currentTimeMillis());
        paymentTransactionRepository.save(transaction);

        return toWalletResponse(savedWallet);
    }

    @Override
    public List<PaymentTransactionResponse> getTransactions(String userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        return paymentTransactionRepository.findByWalletId(wallet.getId())
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .build();
    }

    private PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return PaymentTransactionResponse.builder()
                .id(transaction.getId())
                .walletId(transaction.getWalletId())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .externalReference(transaction.getExternalReference())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}