package com.mysawit.mysawit_pembayaran.service;

import com.mysawit.mysawit_pembayaran.model.Wallet;
import com.mysawit.mysawit_pembayaran.repository.PaymentTransactionRepository;
import com.mysawit.mysawit_pembayaran.repository.WalletRepository;
import com.mysawit.mysawit_pembayaran.service.impl.WalletServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        walletService = new WalletServiceImpl(walletRepository, paymentTransactionRepository);
    }

    @Test
    void topUpWallet_success() {
        Wallet wallet = new Wallet();
        wallet.setId("wallet-1");
        wallet.setUserId("ADMIN");
        wallet.setBalance(new BigDecimal("100"));

        when(walletRepository.findByUserId("ADMIN")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = walletService.topUpWallet("ADMIN", new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), response.getBalance());
        verify(paymentTransactionRepository, times(1)).save(any());
    }

    @Test
    void topUpWallet_invalidAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> walletService.topUpWallet("ADMIN", BigDecimal.ZERO));
    }
}