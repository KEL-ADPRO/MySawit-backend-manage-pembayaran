package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.model.Wallet;
import com.mysawit.pembayaran.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private static final int MONEY_SCALE = 2;

    @Override
    @Transactional
    public WalletResponse getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> createWallet(userId));
    }

    @Override
    @Transactional
    public WalletResponse createWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .userId(userId)
                            .balance(normalizeMoney(BigDecimal.ZERO))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return toResponse(walletRepository.save(wallet));
                });
    }

    @Override
    @Transactional
    public WalletResponse addBalance(UUID userId, BigDecimal amount) {
        BigDecimal safeAmount = normalizeNonNegativeAmount(amount);
        Wallet wallet = getOrCreateWalletForUpdate(userId);
        wallet.setBalance(normalizeMoney(wallet.getBalance().add(safeAmount)));
        wallet.setUpdatedAt(LocalDateTime.now());
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    @Transactional
    public WalletResponse deductBalance(UUID userId, BigDecimal amount) {
        BigDecimal safeAmount = normalizeNonNegativeAmount(amount);
        Wallet wallet = getOrCreateWalletForUpdate(userId);
        if (wallet.getBalance().compareTo(safeAmount) < 0) {
            log.warn("Insufficient balance for user {}: has {}, needs {}", userId, wallet.getBalance(), amount);
            throw new InsufficientBalanceException(
                    "Insufficient balance: current " + wallet.getBalance() + ", requested " + amount);
        }
        wallet.setBalance(normalizeMoney(wallet.getBalance().subtract(safeAmount)));
        wallet.setUpdatedAt(LocalDateTime.now());
        return toResponse(walletRepository.save(wallet));
    }

    private Wallet getOrCreateWalletForUpdate(UUID userId) {
        return walletRepository.findWithLockingByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .balance(normalizeMoney(BigDecimal.ZERO))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    private BigDecimal normalizeNonNegativeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return normalizeMoney(amount);
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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
