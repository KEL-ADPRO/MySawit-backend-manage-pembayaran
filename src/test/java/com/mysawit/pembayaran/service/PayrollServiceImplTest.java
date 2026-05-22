package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.model.Payroll;
import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.model.enums.PayrollKilogramType;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.repository.PayrollRepository;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import com.mysawit.pembayaran.service.strategy.BuruhWageStrategy;
import com.mysawit.pembayaran.service.strategy.MandorWageStrategy;
import com.mysawit.pembayaran.service.strategy.SupirTrukWageStrategy;
import com.mysawit.pembayaran.service.strategy.WageCalculatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private WageConfigRepository wageConfigRepository;

    @Mock
    private WalletService walletService;

    private PayrollServiceImpl payrollService;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollServiceImpl(
                payrollRepository,
                wageConfigRepository,
                new WageCalculatorFactory(List.of(
                        new BuruhWageStrategy(),
                        new SupirTrukWageStrategy(),
                        new MandorWageStrategy())),
                walletService);
        ReflectionTestUtils.setField(payrollService, "exchangeRate", new BigDecimal("10000"));
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private WageConfig wageConfigWith(String buruh, String supir, String mandor) {
        return WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(bd(buruh))
                .supirTrukWagePerKg(bd(supir))
                .mandorWagePerKg(bd(mandor))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Payroll pendingPayroll(UUID id, UUID userId, BigDecimal amount) {
        return Payroll.builder()
                .id(id)
                .userId(userId)
                .userRole(UserRole.BURUH)
                .amount(amount)
                .kilogram(bd("100.000"))
                .harvestedKg(bd("100.000"))
                .kilogramType(PayrollKilogramType.HARVESTED)
                .sourceType(PayrollSourceType.MANUAL_ADMIN)
                .description("test")
                .status(PayrollStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void mockSave() {
        when(payrollRepository.save(any())).thenAnswer(inv -> {
            Payroll payroll = inv.getArgument(0);
            if (payroll.getId() == null) {
                payroll.setId(UUID.randomUUID());
            }
            return payroll;
        });
    }

    @Test
    void createPayroll_buruh_usesHarvestedKgFormula() {
        UUID userId = UUID.randomUUID();
        when(wageConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfigWith("50", "0", "0")));
        mockSave();

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setHarvestedKg(bd("100"));

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getAmount()).isEqualByComparingTo("0.45");
        assertThat(result.getKilogram()).isEqualByComparingTo("100.000");
        assertThat(result.getKilogramType()).isEqualTo(PayrollKilogramType.HARVESTED);
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.PENDING);
    }

    @Test
    void createPayroll_supirTruk_usesDeliveredKgFormula() {
        UUID userId = UUID.randomUUID();
        when(wageConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfigWith("0", "30", "0")));
        mockSave();

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.SUPIR_TRUK);
        request.setDeliveredKg(bd("200"));

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getAmount()).isEqualByComparingTo("0.54");
        assertThat(result.getKilogramType()).isEqualTo(PayrollKilogramType.DELIVERED);
    }

    @Test
    void createPayroll_mandor_usesRecognizedKgFormula() {
        UUID userId = UUID.randomUUID();
        when(wageConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfigWith("0", "0", "40")));
        mockSave();

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.MANDOR);
        request.setRecognizedKg(bd("150"));

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getAmount()).isEqualByComparingTo("0.54");
        assertThat(result.getRecognizedKg()).isEqualByComparingTo("150.000");
        assertThat(result.getKilogramType()).isEqualTo(PayrollKilogramType.RECOGNIZED);
    }

    @Test
    void createPayroll_integrationRequiresMatchingRoleAndSourceMetadata() {
        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(UUID.randomUUID());
        request.setUserRole(UserRole.MANDOR);
        request.setSourceType(PayrollSourceType.HARVEST_APPROVAL);
        request.setHarvestedKg(bd("100"));
        request.setSourceId("harvest-1");
        request.setIdempotencyKey("payroll-harvest-1");

        assertThatThrownBy(() -> payrollService.createPayroll(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role");
    }

    @Test
    void createPayroll_descriptionIsTransparent() {
        when(wageConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfigWith("50", "0", "0")));
        mockSave();

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(UUID.randomUUID());
        request.setUserRole(UserRole.BURUH);
        request.setSourceType(PayrollSourceType.HARVEST_APPROVAL);
        request.setSourceId("harvest-1");
        request.setIdempotencyKey("payroll-harvest-1");
        request.setHarvestedKg(bd("100"));

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getDescription()).contains("BURUH", "HARVEST_APPROVAL", "harvested", "100", "Rp 50", "90%", "0.45 SawitDollar");
    }

    @Test
    void createPayroll_duplicateIdempotencyKey_returnsExistingPayroll() {
        Payroll existing = pendingPayroll(UUID.randomUUID(), UUID.randomUUID(), bd("4500.00"));
        existing.setIdempotencyKey("payroll-harvest-1");
        when(payrollRepository.findByIdempotencyKey("payroll-harvest-1")).thenReturn(Optional.of(existing));

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(existing.getUserId());
        request.setUserRole(UserRole.BURUH);
        request.setSourceType(PayrollSourceType.HARVEST_APPROVAL);
        request.setSourceId("harvest-1");
        request.setIdempotencyKey("payroll-harvest-1");
        request.setHarvestedKg(bd("100"));

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void approvePayroll_creditsRecipientAndDeductsAuthenticatedAdmin() {
        UUID payrollId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, userId, bd("1000.00"));
        when(payrollRepository.findWithLockingById(payrollId)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PayrollResponse result = payrollService.approvePayroll(payrollId, adminId);

        assertThat(result.getStatus()).isEqualTo(PayrollStatus.ACCEPTED);
        verify(walletService).deductBalance(adminId, bd("1000.00"));
        verify(walletService).addBalance(userId, bd("1000.00"));
    }

    @Test
    void approvePayroll_insufficientBalance_keepsPayrollPending() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, UUID.randomUUID(), bd("1000.00"));
        when(payrollRepository.findWithLockingById(payrollId)).thenReturn(Optional.of(payroll));
        doThrow(new InsufficientBalanceException("Insufficient balance"))
                .when(walletService).deductBalance(adminId, bd("1000.00"));

        assertThatThrownBy(() -> payrollService.approvePayroll(payrollId, adminId))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(payroll.getStatus()).isEqualTo(PayrollStatus.PENDING);
        verify(walletService, never()).addBalance(any(), any());
    }

    @Test
    void approvePayroll_twiceOnlyPaysOnce() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, UUID.randomUUID(), bd("1000.00"));
        when(payrollRepository.findWithLockingById(payrollId)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        payrollService.approvePayroll(payrollId, adminId);

        assertThatThrownBy(() -> payrollService.approvePayroll(payrollId, adminId))
                .isInstanceOf(IllegalStateException.class);
        verify(walletService, times(1)).deductBalance(adminId, bd("1000.00"));
    }

    @Test
    void approvePayroll_notFound_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        when(payrollRepository.findWithLockingById(payrollId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.approvePayroll(payrollId, UUID.randomUUID()))
                .isInstanceOf(PayrollNotFoundException.class);
    }

    @Test
    void rejectPayroll_savesReasonAndDoesNotTouchWallet() {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, UUID.randomUUID(), bd("1000.00"));
        when(payrollRepository.findWithLockingById(payrollId)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Quality not met");

        PayrollResponse result = payrollService.rejectPayroll(payrollId, request);

        assertThat(result.getStatus()).isEqualTo(PayrollStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Quality not met");
        verify(walletService, never()).deductBalance(any(), any());
        verify(walletService, never()).addBalance(any(), any());
    }

    @Test
    void rejectPayroll_blankReason_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, UUID.randomUUID(), bd("1000.00"));
        when(payrollRepository.findWithLockingById(payrollId)).thenReturn(Optional.of(payroll));

        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("");

        assertThatThrownBy(() -> payrollService.rejectPayroll(payrollId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPayrolls_filterByUserStatusAndDateRange() {
        UUID userId = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.of(2026, 5, 20, 12, 0);
        Payroll old = pendingPayroll(UUID.randomUUID(), userId, bd("100.00"));
        old.setCreatedAt(base.minusDays(5));
        Payroll recent = pendingPayroll(UUID.randomUUID(), userId, bd("200.00"));
        recent.setCreatedAt(base.plusDays(1));
        when(payrollRepository.findByUserIdAndStatus(userId, PayrollStatus.PENDING)).thenReturn(List.of(old, recent));

        List<PayrollResponse> result = payrollService.getPayrolls(PayrollStatus.PENDING, userId, base, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void getPayrollById_found_shouldReturn() {
        UUID id = UUID.randomUUID();
        Payroll payroll = pendingPayroll(id, UUID.randomUUID(), bd("500.00"));
        when(payrollRepository.findById(id)).thenReturn(Optional.of(payroll));

        PayrollResponse result = payrollService.getPayrollById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getAmount()).isEqualByComparingTo("500.00");
    }
}
