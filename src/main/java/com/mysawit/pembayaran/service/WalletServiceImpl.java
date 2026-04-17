package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.TopUpRequest;
import com.mysawit.pembayaran.dto.response.TopUpResponse;
import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.model.Wallet;
import com.mysawit.pembayaran.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    public WalletResponse deductBalance(UUID userId, double amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
        if (wallet.getBalance() < amount) {
            throw new InsufficientBalanceException(
                    "Insufficient balance: current " + wallet.getBalance() + ", requested " + amount);
        }
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        return toResponse(walletRepository.save(wallet));
    }

    @Override
    public TopUpResponse initiateTopUp(TopUpRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void handleTopUpCallback(Map<String, Object> payload) {
        throw new UnsupportedOperationException("Not yet implemented");
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
