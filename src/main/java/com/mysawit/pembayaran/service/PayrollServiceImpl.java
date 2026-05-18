package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.model.Payroll;
import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.repository.PayrollRepository;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import com.mysawit.pembayaran.service.strategy.WageCalculatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final WageConfigRepository wageConfigRepository;
    private final WageCalculatorFactory wageCalculatorFactory;
    private final WalletService walletService;

    @Override
    public PayrollResponse createPayroll(CreatePayrollRequest request) {
        WageConfig config = wageConfigRepository.findTopByOrderByUpdatedAtDesc()
                .orElse(WageConfig.builder()
                        .buruhWagePerKg(0).supirTrukWagePerKg(0).mandorWagePerKg(0)
                        .updatedAt(LocalDateTime.now()).build());

        double wagePerKg = wageCalculatorFactory.getWagePerKg(request.getUserRole(), config);
        double amount = wageCalculatorFactory.calculate(request.getUserRole(), wagePerKg, request.getKilogram());

        String description = String.format("Payroll %s: %.1f kg x %.1f/kg = %.1f SawitDollar",
                request.getUserRole(), request.getKilogram(), wagePerKg, amount);

        Payroll payroll = Payroll.builder()
                .userId(request.getUserId())
                .userRole(request.getUserRole())
                .kilogram(request.getKilogram())
                .amount(amount)
                .description(description)
                .status(PayrollStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toResponse(payrollRepository.save(payroll));
    }

    @Override
    @Transactional
    public PayrollResponse approvePayroll(UUID id, UUID adminUserId) {
        Payroll payroll = findOrThrow(id);
        if (payroll.getStatus() != PayrollStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payrolls can be approved");
        }
        log.info("Approving payroll {} for user {}, amount {}", id, payroll.getUserId(), payroll.getAmount());
        walletService.deductBalance(adminUserId, payroll.getAmount());
        walletService.addBalance(payroll.getUserId(), payroll.getAmount());
        payroll.setStatus(PayrollStatus.ACCEPTED);
        payroll.setUpdatedAt(LocalDateTime.now());
        return toResponse(payrollRepository.save(payroll));
    }

    @Override
    public PayrollResponse rejectPayroll(UUID id, RejectPayrollRequest request) {
        Payroll payroll = findOrThrow(id);
        if (payroll.getStatus() != PayrollStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payrolls can be rejected");
        }
        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be blank");
        }
        payroll.setStatus(PayrollStatus.REJECTED);
        payroll.setRejectionReason(request.getRejectionReason());
        payroll.setUpdatedAt(LocalDateTime.now());
        return toResponse(payrollRepository.save(payroll));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> getPayrolls(PayrollStatus status, UUID userId,
                                              LocalDateTime startDate, LocalDateTime endDate) {
        List<Payroll> payrolls;

        if (userId != null && status != null) {
            payrolls = payrollRepository.findByUserIdAndStatus(userId, status);
        } else if (userId != null) {
            payrolls = payrollRepository.findByUserId(userId);
        } else if (status != null) {
            payrolls = payrollRepository.findByStatus(status);
        } else {
            payrolls = payrollRepository.findAll();
        }

        return payrolls.stream()
                .filter(p -> startDate == null || !p.getCreatedAt().isBefore(startDate))
                .filter(p -> endDate == null || !p.getCreatedAt().isAfter(endDate))
                .sorted(Comparator.comparing(Payroll::getCreatedAt).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getPayrollById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    private Payroll findOrThrow(UUID id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new PayrollNotFoundException(id));
    }

    private PayrollResponse toResponse(Payroll payroll) {
        return PayrollResponse.builder()
                .id(payroll.getId())
                .userId(payroll.getUserId())
                .userRole(payroll.getUserRole())
                .amount(payroll.getAmount())
                .kilogram(payroll.getKilogram())
                .description(payroll.getDescription())
                .status(payroll.getStatus())
                .rejectionReason(payroll.getRejectionReason())
                .createdAt(payroll.getCreatedAt())
                .updatedAt(payroll.getUpdatedAt())
                .build();
    }
}
