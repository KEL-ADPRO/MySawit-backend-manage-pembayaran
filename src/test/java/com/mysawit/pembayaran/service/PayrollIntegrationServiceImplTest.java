package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.HarvestPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollIntegrationServiceImplTest {

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private WageConfigRepository wageConfigRepository;

    @Mock
    private WalletService walletService;

    private PayrollIntegrationServiceImpl integrationService;

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @BeforeEach
    void setUp() {
        PayrollServiceImpl payrollService = new PayrollServiceImpl(
                payrollRepository,
                wageConfigRepository,
                new WageCalculatorFactory(List.of(
                        new BuruhWageStrategy(),
                        new SupirTrukWageStrategy(),
                        new MandorWageStrategy())),
                walletService);
        ReflectionTestUtils.setField(payrollService, "exchangeRate", new BigDecimal("10000"));
        integrationService = new PayrollIntegrationServiceImpl(payrollService);

        lenient().when(wageConfigRepository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(bd("50"))
                .supirTrukWagePerKg(bd("30"))
                .mandorWagePerKg(bd("40"))
                .updatedAt(LocalDateTime.now())
                .build()));
        lenient().doAnswer(inv -> {
            Payroll payroll = inv.getArgument(0);
            if (payroll.getId() == null) {
                payroll.setId(UUID.randomUUID());
            }
            return payroll;
        }).when(payrollRepository).save(any());
    }

    @Test
    void harvestApprovedEventCreatesBuruhPayrollPending() {
        when(payrollRepository.findByIdempotencyKey("harvest-key")).thenReturn(Optional.empty());

        PayrollResponse result = integrationService.createFromHarvestApproval(harvestRequest("harvest-key", bd("100"))).join();

        assertThat(result.getUserRole()).isEqualTo(UserRole.BURUH);
        assertThat(result.getSourceType()).isEqualTo(PayrollSourceType.HARVEST_APPROVAL);
        assertThat(result.getKilogramType()).isEqualTo(PayrollKilogramType.HARVESTED);
        assertThat(result.getAmount()).isEqualByComparingTo("0.45");
        assertThat(result.getStatus()).isEqualTo(PayrollStatus.PENDING);
    }

    @Test
    void driverDeliveryApprovedEventCreatesSupirPayrollPending() {
        when(payrollRepository.findByIdempotencyKey("driver-key")).thenReturn(Optional.empty());

        DriverDeliveryPayrollEventRequest request = new DriverDeliveryPayrollEventRequest();
        request.setSourceId("delivery-1");
        request.setSupirUserId(UUID.randomUUID());
        request.setDeliveredKg(bd("200"));
        request.setIdempotencyKey("driver-key");

        PayrollResponse result = integrationService.createFromDriverDeliveryApproval(request).join();

        assertThat(result.getUserRole()).isEqualTo(UserRole.SUPIR_TRUK);
        assertThat(result.getSourceType()).isEqualTo(PayrollSourceType.DRIVER_DELIVERY_APPROVAL);
        assertThat(result.getKilogramType()).isEqualTo(PayrollKilogramType.DELIVERED);
        assertThat(result.getAmount()).isEqualByComparingTo("0.54");
    }

    @Test
    void factoryDeliveryApprovedEventCreatesMandorPayrollWithRecognizedKg() {
        when(payrollRepository.findByIdempotencyKey("factory-key")).thenReturn(Optional.empty());

        FactoryDeliveryPayrollEventRequest request = new FactoryDeliveryPayrollEventRequest();
        request.setSourceId("factory-delivery-1");
        request.setMandorUserId(UUID.randomUUID());
        request.setRecognizedKg(bd("80"));
        request.setIdempotencyKey("factory-key");

        PayrollResponse result = integrationService.createFromFactoryDeliveryApproval(request).join();

        assertThat(result.getUserRole()).isEqualTo(UserRole.MANDOR);
        assertThat(result.getSourceType()).isEqualTo(PayrollSourceType.FACTORY_DELIVERY_APPROVAL);
        assertThat(result.getKilogramType()).isEqualTo(PayrollKilogramType.RECOGNIZED);
        assertThat(result.getRecognizedKg()).isEqualByComparingTo("80.000");
        assertThat(result.getAmount()).isEqualByComparingTo("0.29");
    }

    @Test
    void duplicateIdempotencyKeyDoesNotCreateDoublePayroll() {
        AtomicReference<Payroll> savedPayroll = new AtomicReference<>();
        when(payrollRepository.findByIdempotencyKey("harvest-key"))
                .thenAnswer(inv -> Optional.ofNullable(savedPayroll.get()));
        doAnswer(inv -> {
            Payroll payroll = inv.getArgument(0);
            payroll.setId(UUID.randomUUID());
            savedPayroll.set(payroll);
            return payroll;
        }).when(payrollRepository).save(any());

        PayrollResponse first = integrationService.createFromHarvestApproval(harvestRequest("harvest-key", bd("100"))).join();
        PayrollResponse second = integrationService.createFromHarvestApproval(harvestRequest("harvest-key", bd("100"))).join();

        assertThat(second.getId()).isEqualTo(first.getId());
        verify(payrollRepository, times(1)).save(any());
    }

    @Test
    void invalidKilogramIsRejected() {
        when(payrollRepository.findByIdempotencyKey("harvest-key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> integrationService.createFromHarvestApproval(harvestRequest("harvest-key", bd("-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    private HarvestPayrollEventRequest harvestRequest(String idempotencyKey, BigDecimal harvestedKg) {
        HarvestPayrollEventRequest request = new HarvestPayrollEventRequest();
        request.setSourceId("harvest-1");
        request.setBuruhUserId(UUID.randomUUID());
        request.setHarvestedKg(harvestedKg);
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }
}
