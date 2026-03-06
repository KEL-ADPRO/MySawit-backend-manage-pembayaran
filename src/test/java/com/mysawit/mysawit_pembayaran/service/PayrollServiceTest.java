package com.mysawit.mysawit_pembayaran.service;

import com.mysawit.mysawit_pembayaran.exception.InsufficientBalanceException;
import com.mysawit.mysawit_pembayaran.exception.PayrollNotFoundException;
import com.mysawit.mysawit_pembayaran.model.Payroll;
import com.mysawit.mysawit_pembayaran.model.PayrollStatus;
import com.mysawit.mysawit_pembayaran.model.Wallet;
import com.mysawit.mysawit_pembayaran.repository.PayrollRepository;
import com.mysawit.mysawit_pembayaran.repository.WalletRepository;
import com.mysawit.mysawit_pembayaran.service.impl.PayrollServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PayrollServiceTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private WalletRepository walletRepository;

    private PayrollServiceImpl payrollService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        payrollService = new PayrollServiceImpl(payrollRepository, walletRepository);
    }

    @Test
    void approvePayroll_success() {
        Payroll payroll = new Payroll();
        payroll.setId("payroll-1");
        payroll.setRecipientUserId("user-1");
        payroll.setRecipientRole("BURUH");
        payroll.setAmount(new BigDecimal("100"));
        payroll.setDescription("Payroll buruh");

        Wallet adminWallet = new Wallet();
        adminWallet.setUserId("ADMIN");
        adminWallet.setBalance(new BigDecimal("500"));

        Wallet recipientWallet = new Wallet();
        recipientWallet.setUserId("user-1");
        recipientWallet.setBalance(BigDecimal.ZERO);

        when(payrollRepository.findById("payroll-1")).thenReturn(Optional.of(payroll));
        when(walletRepository.findByUserId("ADMIN")).thenReturn(Optional.of(adminWallet));
        when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(recipientWallet));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = payrollService.approvePayroll("payroll-1");

        assertEquals(PayrollStatus.ACCEPTED, response.getStatus());
        assertEquals(new BigDecimal("400"), adminWallet.getBalance());
        assertEquals(new BigDecimal("100"), recipientWallet.getBalance());
    }

    @Test
    void approvePayroll_payrollNotFound() {
        when(payrollRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(PayrollNotFoundException.class, () -> payrollService.approvePayroll("missing"));
    }

    @Test
    void approvePayroll_insufficientAdminBalance() {
        Payroll payroll = new Payroll();
        payroll.setId("payroll-1");
        payroll.setRecipientUserId("user-1");
        payroll.setAmount(new BigDecimal("1000"));
        payroll.setDescription("Payroll besar");

        Wallet adminWallet = new Wallet();
        adminWallet.setUserId("ADMIN");
        adminWallet.setBalance(new BigDecimal("10"));

        Wallet recipientWallet = new Wallet();
        recipientWallet.setUserId("user-1");
        recipientWallet.setBalance(BigDecimal.ZERO);

        when(payrollRepository.findById("payroll-1")).thenReturn(Optional.of(payroll));
        when(walletRepository.findByUserId("ADMIN")).thenReturn(Optional.of(adminWallet));
        when(walletRepository.findByUserId("user-1")).thenReturn(Optional.of(recipientWallet));

        assertThrows(InsufficientBalanceException.class, () -> payrollService.approvePayroll("payroll-1"));
    }

    @Test
    void rejectPayroll_withoutReason() {
        Payroll payroll = new Payroll();
        payroll.setId("payroll-1");

        when(payrollRepository.findById("payroll-1")).thenReturn(Optional.of(payroll));

        assertThrows(IllegalArgumentException.class, () -> payrollService.rejectPayroll("payroll-1", ""));
    }
}