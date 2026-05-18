package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.CreatePayrollRequest;
import com.mysawit.pembayaran.dto.request.RejectPayrollRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.dto.response.WalletResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.model.Payroll;
import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.repository.PayrollRepository;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import com.mysawit.pembayaran.service.strategy.WageCalculatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private WageCalculatorFactory wageCalculatorFactory;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private PayrollServiceImpl payrollService;

    private WageConfig wageConfigWith(double buruh, double supir, double mandor) {
        return WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(buruh)
                .supirTrukWagePerKg(supir)
                .mandorWagePerKg(mandor)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Payroll pendingPayroll(UUID id, UUID userId, double amount) {
        return pendingPayroll(id, userId, UserRole.BURUH, amount);
    }

    private Payroll pendingPayroll(UUID id, UUID userId, UserRole role, double amount) {
        return Payroll.builder()
                .id(id)
                .userId(userId)
                .userRole(role)
                .amount(amount)
                .kilogram(100.0)
                .status(PayrollStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── CREATE TESTS ───────────────────────────────────────────────────────

    @Test
    void createPayroll_buruh_shouldCalculateCorrectAmount() {
        UUID userId = UUID.randomUUID();
        WageConfig wageConfig = wageConfigWith(5000.0, 0, 0);
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfig));
        when(wageCalculatorFactory.getWagePerKg(UserRole.BURUH, wageConfig)).thenReturn(5000.0);
        when(wageCalculatorFactory.calculate(UserRole.BURUH, 5000.0, 100.0)).thenReturn(450000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setKilogram(100.0);

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getAmount()).isEqualTo(450000.0);
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.PENDING);
    }

    @Test
    void createPayroll_supirTruk_shouldCalculateCorrectAmount() {
        UUID userId = UUID.randomUUID();
        WageConfig wageConfig = wageConfigWith(0, 3000.0, 0);
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfig));
        when(wageCalculatorFactory.getWagePerKg(UserRole.SUPIR_TRUK, wageConfig)).thenReturn(3000.0);
        when(wageCalculatorFactory.calculate(UserRole.SUPIR_TRUK, 3000.0, 200.0)).thenReturn(540000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.SUPIR_TRUK);
        request.setKilogram(200.0);

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getAmount()).isEqualTo(540000.0);
    }

    @Test
    void createPayroll_mandor_shouldCalculateCorrectAmount() {
        UUID userId = UUID.randomUUID();
        WageConfig wageConfig = wageConfigWith(0, 0, 4000.0);
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfig));
        when(wageCalculatorFactory.getWagePerKg(UserRole.MANDOR, wageConfig)).thenReturn(4000.0);
        when(wageCalculatorFactory.calculate(UserRole.MANDOR, 4000.0, 150.0)).thenReturn(540000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.MANDOR);
        request.setKilogram(150.0);

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getAmount()).isEqualTo(540000.0);
    }

    @Test
    void createPayroll_shouldSetStatusPending() {
        UUID userId = UUID.randomUUID();
        WageConfig wageConfig = wageConfigWith(5000.0, 0, 0);
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfig));
        when(wageCalculatorFactory.getWagePerKg(eq(UserRole.BURUH), eq(wageConfig))).thenReturn(5000.0);
        when(wageCalculatorFactory.calculate(any(), anyDouble(), anyDouble())).thenReturn(450000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setKilogram(100.0);

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getStatus()).isEqualTo(PayrollStatus.PENDING);
    }

    @Test
    void createPayroll_shouldUseLatestWageConfig() {
        UUID userId = UUID.randomUUID();
        WageConfig latest = wageConfigWith(7000.0, 0, 0);
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc())
                .thenReturn(Optional.of(latest));
        when(wageCalculatorFactory.getWagePerKg(UserRole.BURUH, latest)).thenReturn(7000.0);
        when(wageCalculatorFactory.calculate(UserRole.BURUH, 7000.0, 50.0)).thenReturn(315000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setKilogram(50.0);

        PayrollResponse result = payrollService.createPayroll(request);

        verify(wageConfigRepository).findTopByOrderByUpdatedAtDesc();
        verify(wageCalculatorFactory).getWagePerKg(UserRole.BURUH, latest);
        verify(wageCalculatorFactory).calculate(UserRole.BURUH, 7000.0, 50.0);
        assertThat(result.getAmount()).isEqualTo(315000.0);
    }

    @Test
    void createPayroll_shouldGenerateDescription() {
        UUID userId = UUID.randomUUID();
        WageConfig wageConfig = wageConfigWith(5000.0, 0, 0);
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(wageConfig));
        when(wageCalculatorFactory.getWagePerKg(UserRole.BURUH, wageConfig)).thenReturn(5000.0);
        when(wageCalculatorFactory.calculate(UserRole.BURUH, 5000.0, 100.0)).thenReturn(450000.0);
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreatePayrollRequest request = new CreatePayrollRequest();
        request.setUserId(userId);
        request.setUserRole(UserRole.BURUH);
        request.setKilogram(100.0);

        PayrollResponse result = payrollService.createPayroll(request);

        assertThat(result.getDescription()).isNotBlank();
        assertThat(result.getDescription()).containsIgnoringCase("100");
        assertThat(result.getDescription()).containsIgnoringCase("450000");
    }

    // ─── APPROVE/REJECT TESTS ────────────────────────────────────────────────

    @Test
    void approvePayroll_success() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, userId, 1000.0);
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletService.deductBalance(adminId, 1000.0))
                .thenReturn(WalletResponse.builder().balance(4000.0).build());
        when(walletService.addBalance(userId, 1000.0))
                .thenReturn(WalletResponse.builder().balance(1000.0).build());

        PayrollResponse result = payrollService.approvePayroll(payrollId, adminId);

        assertThat(result.getStatus()).isEqualTo(PayrollStatus.ACCEPTED);
        verify(walletService).deductBalance(adminId, 1000.0);
        verify(walletService).addBalance(userId, 1000.0);
    }

    @Test
    void approvePayroll_supirTruk_success() {
        assertApprovePayrollForRole(UserRole.SUPIR_TRUK);
    }

    @Test
    void approvePayroll_mandor_success() {
        assertApprovePayrollForRole(UserRole.MANDOR);
    }

    @Test
    void approvePayroll_insufficientBalance_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, userId, 1000.0);
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));
        doThrow(new InsufficientBalanceException("Insufficient balance"))
                .when(walletService).deductBalance(adminId, 1000.0);

        assertThatThrownBy(() -> payrollService.approvePayroll(payrollId, adminId))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void approvePayroll_notPending_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = Payroll.builder()
                .id(payrollId)
                .userId(UUID.randomUUID())
                .status(PayrollStatus.ACCEPTED)
                .amount(1000.0)
                .kilogram(100.0)
                .userRole(UserRole.BURUH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

        assertThatThrownBy(() -> payrollService.approvePayroll(payrollId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approvePayroll_rejected_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = Payroll.builder()
                .id(payrollId)
                .userId(UUID.randomUUID())
                .status(PayrollStatus.REJECTED)
                .amount(1000.0)
                .kilogram(100.0)
                .userRole(UserRole.BURUH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

        assertThatThrownBy(() -> payrollService.approvePayroll(payrollId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approvePayroll_notFound_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.approvePayroll(payrollId, UUID.randomUUID()))
                .isInstanceOf(PayrollNotFoundException.class);
    }

    @Test
    void rejectPayroll_success() {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, UUID.randomUUID(), 1000.0);
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Quality not met");

        PayrollResponse result = payrollService.rejectPayroll(payrollId, request);

        assertThat(result.getStatus()).isEqualTo(PayrollStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Quality not met");
        verify(walletService, never()).deductBalance(any(), anyDouble());
        verify(walletService, never()).addBalance(any(), anyDouble());
    }

    @Test
    void rejectPayroll_supirTruk_success() {
        assertRejectPayrollForRole(UserRole.SUPIR_TRUK);
    }

    @Test
    void rejectPayroll_mandor_success() {
        assertRejectPayrollForRole(UserRole.MANDOR);
    }

    @Test
    void rejectPayroll_blankReason_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, UUID.randomUUID(), 1000.0);
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("");

        assertThatThrownBy(() -> payrollService.rejectPayroll(payrollId, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectPayroll_notPending_shouldThrow() {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = Payroll.builder()
                .id(payrollId)
                .userId(UUID.randomUUID())
                .status(PayrollStatus.REJECTED)
                .amount(1000.0)
                .kilogram(100.0)
                .userRole(UserRole.BURUH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));

        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Some reason");

        assertThatThrownBy(() -> payrollService.rejectPayroll(payrollId, request))
                .isInstanceOf(IllegalStateException.class);
    }

    private void assertApprovePayrollForRole(UserRole role) {
        UUID payrollId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, userId, role, 1000.0);
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletService.deductBalance(adminId, 1000.0))
                .thenReturn(WalletResponse.builder().balance(4000.0).build());
        when(walletService.addBalance(userId, 1000.0))
                .thenReturn(WalletResponse.builder().balance(1000.0).build());

        PayrollResponse result = payrollService.approvePayroll(payrollId, adminId);

        assertThat(result.getStatus()).isEqualTo(PayrollStatus.ACCEPTED);
        assertThat(result.getUserRole()).isEqualTo(role);
        verify(walletService).deductBalance(adminId, 1000.0);
        verify(walletService).addBalance(userId, 1000.0);
    }

    private void assertRejectPayrollForRole(UserRole role) {
        UUID payrollId = UUID.randomUUID();
        Payroll payroll = pendingPayroll(payrollId, UUID.randomUUID(), role, 1000.0);
        when(payrollRepository.findById(payrollId)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RejectPayrollRequest request = new RejectPayrollRequest();
        request.setRejectionReason("Quality not met");

        PayrollResponse result = payrollService.rejectPayroll(payrollId, request);

        assertThat(result.getStatus()).isEqualTo(PayrollStatus.REJECTED);
        assertThat(result.getUserRole()).isEqualTo(role);
        assertThat(result.getRejectionReason()).isEqualTo("Quality not met");
        verify(walletService, never()).deductBalance(any(), anyDouble());
        verify(walletService, never()).addBalance(any(), anyDouble());
    }

    // ─── LIST / DETAIL TESTS ────────────────────────────────────────────────

    @Test
    void getPayrolls_noFilter_shouldReturnAll() {
        List<Payroll> all = List.of(
                pendingPayroll(UUID.randomUUID(), UUID.randomUUID(), 100.0),
                pendingPayroll(UUID.randomUUID(), UUID.randomUUID(), 200.0)
        );
        when(payrollRepository.findAll()).thenReturn(all);

        List<PayrollResponse> result = payrollService.getPayrolls(null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void getPayrolls_filterByUserId() {
        UUID userId = UUID.randomUUID();
        List<Payroll> userPayrolls = List.of(pendingPayroll(UUID.randomUUID(), userId, 100.0));
        when(payrollRepository.findByUserId(userId)).thenReturn(userPayrolls);

        List<PayrollResponse> result = payrollService.getPayrolls(null, userId, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void getPayrolls_filterByStatus() {
        List<Payroll> pendingPayrolls = List.of(
                pendingPayroll(UUID.randomUUID(), UUID.randomUUID(), 100.0)
        );
        when(payrollRepository.findByStatus(PayrollStatus.PENDING)).thenReturn(pendingPayrolls);

        List<PayrollResponse> result = payrollService.getPayrolls(PayrollStatus.PENDING, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PayrollStatus.PENDING);
    }

    @Test
    void getPayrolls_filterByDateRange() {
        LocalDateTime base = LocalDateTime.of(2025, 1, 15, 12, 0);
        Payroll old = Payroll.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .userRole(UserRole.BURUH).amount(100.0).kilogram(10.0)
                .status(PayrollStatus.PENDING)
                .createdAt(base.minusDays(5)).updatedAt(base.minusDays(5))
                .build();
        Payroll recent = Payroll.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .userRole(UserRole.BURUH).amount(200.0).kilogram(20.0)
                .status(PayrollStatus.PENDING)
                .createdAt(base.plusDays(1)).updatedAt(base.plusDays(1))
                .build();
        when(payrollRepository.findAll()).thenReturn(List.of(old, recent));

        List<PayrollResponse> result = payrollService.getPayrolls(null, null, base, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(200.0);
    }

    @Test
    void getPayrollById_found_shouldReturn() {
        UUID id = UUID.randomUUID();
        Payroll payroll = pendingPayroll(id, UUID.randomUUID(), 500.0);
        when(payrollRepository.findById(id)).thenReturn(Optional.of(payroll));

        PayrollResponse result = payrollService.getPayrollById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getAmount()).isEqualTo(500.0);
    }

    @Test
    void getPayrollById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(payrollRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payrollService.getPayrollById(id))
                .isInstanceOf(PayrollNotFoundException.class);
    }
}
