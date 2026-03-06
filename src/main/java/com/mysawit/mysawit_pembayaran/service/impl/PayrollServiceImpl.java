package com.mysawit.mysawit_pembayaran.service.impl;

import com.mysawit.mysawit_pembayaran.dto.response.PayrollResponse;
import com.mysawit.mysawit_pembayaran.exception.InsufficientBalanceException;
import com.mysawit.mysawit_pembayaran.exception.InvalidPayrollStateException;
import com.mysawit.mysawit_pembayaran.exception.PayrollNotFoundException;
import com.mysawit.mysawit_pembayaran.exception.WalletNotFoundException;
import com.mysawit.mysawit_pembayaran.model.Payroll;
import com.mysawit.mysawit_pembayaran.model.PayrollStatus;
import com.mysawit.mysawit_pembayaran.model.Wallet;
import com.mysawit.mysawit_pembayaran.repository.PayrollRepository;
import com.mysawit.mysawit_pembayaran.repository.WalletRepository;
import com.mysawit.mysawit_pembayaran.service.PayrollService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final String ADMIN_USER_ID = "ADMIN";

    private final PayrollRepository payrollRepository;
    private final WalletRepository walletRepository;

    @Override
    public List<PayrollResponse> getPayrolls(String userId, String status) {
        List<Payroll> payrolls;

        if (status == null || status.isBlank()) {
            payrolls = payrollRepository.findByRecipientUserId(userId);
        } else {
            PayrollStatus payrollStatus = PayrollStatus.valueOf(status.toUpperCase());
            payrolls = payrollRepository.findByRecipientUserIdAndStatus(userId, payrollStatus);
        }

        return payrolls.stream().map(this::toResponse).toList();
    }

    @Override
    public PayrollResponse getPayrollById(String payrollId) {
        Payroll payroll = findPayroll(payrollId);
        return toResponse(payroll);
    }

    @Override
    public PayrollResponse approvePayroll(String payrollId) {
        Payroll payroll = findPayroll(payrollId);

        if (payroll.getStatus() != PayrollStatus.PENDING) {
            throw new InvalidPayrollStateException("Only pending payroll can be approved");
        }

        Wallet adminWallet = walletRepository.findByUserId(ADMIN_USER_ID)
                .orElseThrow(() -> new WalletNotFoundException(ADMIN_USER_ID));

        Wallet recipientWallet = walletRepository.findByUserId(payroll.getRecipientUserId())
                .orElseThrow(() -> new WalletNotFoundException(payroll.getRecipientUserId()));

        if (adminWallet.getBalance().compareTo(payroll.getAmount()) < 0) {
            throw new InsufficientBalanceException();
        }

        adminWallet.setBalance(adminWallet.getBalance().subtract(payroll.getAmount()));
        recipientWallet.setBalance(recipientWallet.getBalance().add(payroll.getAmount()));

        walletRepository.save(adminWallet);
        walletRepository.save(recipientWallet);

        payroll.setStatus(PayrollStatus.ACCEPTED);
        payroll.setApprovedAt(LocalDateTime.now());

        return toResponse(payrollRepository.save(payroll));
    }

    @Override
    public PayrollResponse rejectPayroll(String payrollId, String reason) {
        Payroll payroll = findPayroll(payrollId);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason must not be blank");
        }

        if (payroll.getStatus() != PayrollStatus.PENDING) {
            throw new InvalidPayrollStateException("Only pending payroll can be rejected");
        }

        payroll.setStatus(PayrollStatus.REJECTED);
        payroll.setRejectionReason(reason);

        return toResponse(payrollRepository.save(payroll));
    }

    private Payroll findPayroll(String payrollId) {
        return payrollRepository.findById(payrollId)
                .orElseThrow(() -> new PayrollNotFoundException(payrollId));
    }

    private PayrollResponse toResponse(Payroll payroll) {
        return PayrollResponse.builder()
                .id(payroll.getId())
                .recipientUserId(payroll.getRecipientUserId())
                .recipientRole(payroll.getRecipientRole())
                .amount(payroll.getAmount())
                .description(payroll.getDescription())
                .status(payroll.getStatus())
                .rejectionReason(payroll.getRejectionReason())
                .createdAt(payroll.getCreatedAt())
                .approvedAt(payroll.getApprovedAt())
                .build();
    }
}