package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.model.Wallet;
import com.mysawit.pembayaran.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet buildWallet(UUID userId, double balance) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(balance)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWallet_success() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.createWallet(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBalance()).isEqualTo(0.0);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_alreadyExists_shouldReturnExisting() {
        UUID userId = UUID.randomUUID();
        Wallet existing = buildWallet(userId, 500.0);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        WalletResponse result = walletService.createWallet(userId);

        assertThat(result.getBalance()).isEqualTo(500.0);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getWallet_found_shouldReturn() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, 200.0);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletResponse result = walletService.getWalletByUserId(userId);

        assertThat(result.getBalance()).isEqualTo(200.0);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void getWallet_notFound_shouldLazyCreate() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.getWalletByUserId(userId);

        assertThat(result.getBalance()).isEqualTo(0.0);
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void addBalance_success() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, 100.0);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.addBalance(userId, 50.0);

        assertThat(result.getBalance()).isEqualTo(150.0);
    }

    @Test
    void deductBalance_success() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, 100.0);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.deductBalance(userId, 30.0);

        assertThat(result.getBalance()).isEqualTo(70.0);
    }

    @Test
    void deductBalance_insufficientBalance_shouldThrow() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, 50.0);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.deductBalance(userId, 100.0))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void deductBalance_exactBalance_shouldSucceed() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, 100.0);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.deductBalance(userId, 100.0);

        assertThat(result.getBalance()).isEqualTo(0.0);
    }
}
