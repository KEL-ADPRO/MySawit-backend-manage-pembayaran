package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.model.Payroll;
import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.model.enums.PayrollKilogramType;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.repository.PayrollRepository;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import com.mysawit.pembayaran.service.strategy.WageCalculatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private static final int MONEY_SCALE = 2;
    private static final int KG_SCALE = 3;

    private final PayrollRepository payrollRepository;
    private final WageConfigRepository wageConfigRepository;
    private final WageCalculatorFactory wageCalculatorFactory;
    private final WalletService walletService;

    @Override
    @Transactional
    public PayrollResponse createPayroll(CreatePayrollRequest request) {
        if (request.getUserRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin cannot be a payroll recipient");
        }

        PayrollSourceType sourceType = request.getSourceType() == null
                ? PayrollSourceType.MANUAL_ADMIN
                : request.getSourceType();
        validateSourceContract(sourceType, request);

        if (hasText(request.getIdempotencyKey())) {
            return payrollRepository.findByIdempotencyKey(request.getIdempotencyKey().trim())
                    .map(this::toResponse)
                    .orElseGet(() -> createNewPayroll(request, sourceType));
        }

        return createNewPayroll(request, sourceType);
    }

    private PayrollResponse createNewPayroll(CreatePayrollRequest request, PayrollSourceType sourceType) {
        PayrollKilogramContext kilogramContext = resolveKilogramContext(request, sourceType);
        WageConfig config = wageConfigRepository.findFirstBy()
                .orElse(WageConfig.builder()
                        .buruhWagePerKg(normalizeMoney(BigDecimal.ZERO))
                        .supirTrukWagePerKg(normalizeMoney(BigDecimal.ZERO))
                        .mandorWagePerKg(normalizeMoney(BigDecimal.ZERO))
                        .updatedAt(LocalDateTime.now()).build());

        BigDecimal wagePerKg = resolveWagePerKg(config, request.getUserRole());
        BigDecimal amount = normalizeMoney(wageCalculatorFactory.calculate(
                request.getUserRole(), wagePerKg, kilogramContext.kilogram()));

        String description = buildDescription(
                request.getUserRole(), sourceType, kilogramContext.kilogramType(),
                kilogramContext.kilogram(), wagePerKg, amount);

        Payroll payroll = Payroll.builder()
                .userId(request.getUserId())
                .userRole(request.getUserRole())
                .kilogram(kilogramContext.kilogram())
                .harvestedKg(kilogramContext.harvestedKg())
                .deliveredKg(kilogramContext.deliveredKg())
                .recognizedKg(kilogramContext.recognizedKg())
                .kilogramType(kilogramContext.kilogramType())
                .sourceType(sourceType)
                .sourceId(trimToNull(request.getSourceId()))
                .idempotencyKey(trimToNull(request.getIdempotencyKey()))
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
        Payroll payroll = findOrThrowForUpdate(id);
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
    @Transactional
    public PayrollResponse rejectPayroll(UUID id, RejectPayrollRequest request) {
        Payroll payroll = findOrThrowForUpdate(id);
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

    private Payroll findOrThrowForUpdate(UUID id) {
        return payrollRepository.findWithLockingById(id)
                .orElseThrow(() -> new PayrollNotFoundException(id));
    }

    private BigDecimal resolveWagePerKg(WageConfig config, UserRole role) {
        return switch (role) {
            case BURUH -> config.getBuruhWagePerKg();
            case SUPIR_TRUK -> config.getSupirTrukWagePerKg();
            case MANDOR -> config.getMandorWagePerKg();
            case ADMIN -> throw new IllegalArgumentException("Admin cannot receive payroll");
        };
    }

    private void validateSourceContract(PayrollSourceType sourceType, CreatePayrollRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getUserRole() == null) {
            throw new IllegalArgumentException("userRole is required");
        }

        switch (sourceType) {
            case HARVEST_APPROVAL -> validateIntegrationSource(request, UserRole.BURUH);
            case DRIVER_DELIVERY_APPROVAL -> validateIntegrationSource(request, UserRole.SUPIR_TRUK);
            case FACTORY_DELIVERY_APPROVAL -> validateIntegrationSource(request, UserRole.MANDOR);
            case MANUAL_ADMIN -> {
                if (request.getUserRole() == UserRole.ADMIN) {
                    throw new IllegalArgumentException("Manual payroll recipient must be a worker role");
                }
            }
        }
    }

    private void validateIntegrationSource(CreatePayrollRequest request, UserRole expectedRole) {
        if (request.getUserRole() != expectedRole) {
            throw new IllegalArgumentException("Payroll role must match source type");
        }
        if (!hasText(request.getSourceId())) {
            throw new IllegalArgumentException("sourceId is required for integration payroll");
        }
        if (!hasText(request.getIdempotencyKey())) {
            throw new IllegalArgumentException("idempotencyKey is required for integration payroll");
        }
    }

    private PayrollKilogramContext resolveKilogramContext(CreatePayrollRequest request, PayrollSourceType sourceType) {
        return switch (sourceType) {
            case HARVEST_APPROVAL -> harvestedContext(requirePositiveKg(request.getHarvestedKg(), "harvestedKg"));
            case DRIVER_DELIVERY_APPROVAL -> deliveredContext(requirePositiveKg(request.getDeliveredKg(), "deliveredKg"));
            case FACTORY_DELIVERY_APPROVAL -> recognizedContext(requirePositiveKg(request.getRecognizedKg(), "recognizedKg"));
            case MANUAL_ADMIN -> resolveManualKilogramContext(request);
        };
    }

    private PayrollKilogramContext resolveManualKilogramContext(CreatePayrollRequest request) {
        return switch (request.getUserRole()) {
            case BURUH -> harvestedContext(requirePositiveKg(firstNonNull(request.getHarvestedKg(), request.getKilogram()), "harvestedKg"));
            case SUPIR_TRUK -> deliveredContext(requirePositiveKg(firstNonNull(request.getDeliveredKg(), request.getKilogram()), "deliveredKg"));
            case MANDOR -> recognizedContext(requirePositiveKg(firstNonNull(request.getRecognizedKg(), request.getKilogram()), "recognizedKg"));
            case ADMIN -> throw new IllegalArgumentException("Admin cannot receive payroll");
        };
    }

    private PayrollKilogramContext harvestedContext(BigDecimal harvestedKg) {
        return new PayrollKilogramContext(harvestedKg, PayrollKilogramType.HARVESTED, harvestedKg, null, null);
    }

    private PayrollKilogramContext deliveredContext(BigDecimal deliveredKg) {
        return new PayrollKilogramContext(deliveredKg, PayrollKilogramType.DELIVERED, null, deliveredKg, null);
    }

    private PayrollKilogramContext recognizedContext(BigDecimal recognizedKg) {
        return new PayrollKilogramContext(recognizedKg, PayrollKilogramType.RECOGNIZED, null, null, recognizedKg);
    }

    private BigDecimal requirePositiveKg(BigDecimal kilogram, String fieldName) {
        if (kilogram == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (kilogram.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return kilogram.setScale(KG_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal firstNonNull(BigDecimal specificKg, BigDecimal legacyKg) {
        return specificKg != null ? specificKg : legacyKg;
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String buildDescription(UserRole role, PayrollSourceType sourceType, PayrollKilogramType kilogramType,
                                    BigDecimal kilogram, BigDecimal wagePerKg, BigDecimal amount) {
        return "Payroll " + role
                + " dari " + sourceType
                + ": " + format(kilogram) + " kg " + kilogramType.name().toLowerCase()
                + " x " + format(wagePerKg) + " SawitDollar/kg"
                + " x 90% = " + format(amount) + " SawitDollar";
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private PayrollResponse toResponse(Payroll payroll) {
        return PayrollResponse.builder()
                .id(payroll.getId())
                .userId(payroll.getUserId())
                .userRole(payroll.getUserRole())
                .amount(payroll.getAmount())
                .kilogram(payroll.getKilogram())
                .harvestedKg(payroll.getHarvestedKg())
                .deliveredKg(payroll.getDeliveredKg())
                .recognizedKg(payroll.getRecognizedKg())
                .kilogramType(payroll.getKilogramType())
                .sourceType(payroll.getSourceType())
                .sourceId(payroll.getSourceId())
                .idempotencyKey(payroll.getIdempotencyKey())
                .description(payroll.getDescription())
                .status(payroll.getStatus())
                .rejectionReason(payroll.getRejectionReason())
                .createdAt(payroll.getCreatedAt())
                .updatedAt(payroll.getUpdatedAt())
                .build();
    }

    private record PayrollKilogramContext(BigDecimal kilogram,
                                          PayrollKilogramType kilogramType,
                                          BigDecimal harvestedKg,
                                          BigDecimal deliveredKg,
                                          BigDecimal recognizedKg) {
    }
}
