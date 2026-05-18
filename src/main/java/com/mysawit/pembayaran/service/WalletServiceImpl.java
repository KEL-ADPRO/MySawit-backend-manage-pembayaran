package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.WalletNotFoundException;
import com.mysawit.pembayaran.model.Wallet;
import com.mysawit.pembayaran.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    public WalletResponse getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> createWallet(userId));
    }

    @Override
    public WalletResponse createWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .userId(userId)
                            .balance(0.0)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return toResponse(walletRepository.save(wallet));
                });
    }

    @Override
    public WalletResponse addBalance(UUID userId, double amount) {
<<<<<<< HEAD
        Wallet wallet = findWalletByUserId(userId);
=======
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> Wallet.builder()
                        .userId(userId)
                        .balance(0.0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
>>>>>>> origin/staging
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    public WalletResponse deductBalance(UUID userId, double amount) {
<<<<<<< HEAD
        Wallet wallet = findWalletByUserId(userId);
=======
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Deduct requested for non-existent wallet user {}", userId);
                    return new InsufficientBalanceException(
                            "Insufficient balance: wallet not found for user " + userId);
                });
>>>>>>> origin/staging
        if (wallet.getBalance() < amount) {
            log.warn("Insufficient balance for user {}: has {}, needs {}", userId, wallet.getBalance(), amount);
            throw new InsufficientBalanceException(
                    "Insufficient balance: current " + wallet.getBalance() + ", requested " + amount);
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return toResponse(walletRepository.save(wallet));
    }

    private Wallet findWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
