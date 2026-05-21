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

import java.math.BigDecimal;
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

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private Wallet buildWallet(UUID userId, String balance) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(bd(balance))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWallet_successDefaultsToZero() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.createWallet(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBalance()).isEqualByComparingTo("0.00");
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_alreadyExists_shouldReturnExisting() {
        UUID userId = UUID.randomUUID();
        Wallet existing = buildWallet(userId, "500.00");
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        WalletResponse result = walletService.createWallet(userId);

        assertThat(result.getBalance()).isEqualByComparingTo("500.00");
        verify(walletRepository, never()).save(any());
    }

    @Test
    void getWallet_notFound_shouldLazyCreate() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.getWalletByUserId(userId);

        assertThat(result.getBalance()).isEqualByComparingTo("0.00");
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void addBalance_successUsesBigDecimalPrecision() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, "0.10");
        when(walletRepository.findWithLockingByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.addBalance(userId, bd("0.20"));

        assertThat(result.getBalance()).isEqualByComparingTo("0.30");
    }

    @Test
    void addBalance_createsWalletIfMissing() {
        UUID userId = UUID.randomUUID();
        when(walletRepository.findWithLockingByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.addBalance(userId, bd("10.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void deductBalance_success() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, "100.00");
        when(walletRepository.findWithLockingByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.deductBalance(userId, bd("30.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void deductBalance_insufficientBalance_shouldThrowAndNotGoNegative() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, "50.00");
        when(walletRepository.findWithLockingByUserId(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.deductBalance(userId, bd("100.00")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(wallet.getBalance()).isEqualByComparingTo("50.00");
        verify(walletRepository, never()).save(any());
    }

    @Test
    void deductBalance_exactBalance_shouldSucceed() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = buildWallet(userId, "100.00");
        when(walletRepository.findWithLockingByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse result = walletService.deductBalance(userId, bd("100.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("0.00");
    }
}
