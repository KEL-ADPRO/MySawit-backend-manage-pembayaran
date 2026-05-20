package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.repository.WageConfigRepository;
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
class WageConfigServiceImplTest {

    @Mock
    private WageConfigRepository wageConfigRepository;

    @InjectMocks
    private WageConfigServiceImpl wageConfigService;

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void getConfig_exists_shouldReturn() {
        WageConfig config = WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(bd("1000"))
                .supirTrukWagePerKg(bd("1500"))
                .mandorWagePerKg(bd("2000"))
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigRepository.findFirstBy()).thenReturn(Optional.of(config));

        WageConfigResponse result = wageConfigService.getWageConfig();

        assertThat(result.getBuruhWagePerKg()).isEqualByComparingTo("1000");
        assertThat(result.getSupirTrukWagePerKg()).isEqualByComparingTo("1500");
        assertThat(result.getMandorWagePerKg()).isEqualByComparingTo("2000");
    }

    @Test
    void getConfig_notExists_shouldAutoCreateDefault() {
        when(wageConfigRepository.findFirstBy()).thenReturn(Optional.empty());
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WageConfigResponse result = wageConfigService.getWageConfig();

        assertThat(result.getBuruhWagePerKg()).isEqualByComparingTo("0.00");
        assertThat(result.getSupirTrukWagePerKg()).isEqualByComparingTo("0.00");
        assertThat(result.getMandorWagePerKg()).isEqualByComparingTo("0.00");
        verify(wageConfigRepository).save(any(WageConfig.class));
    }

    @Test
    void updateConfig_success() {
        WageConfig existing = WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(bd("1000"))
                .supirTrukWagePerKg(bd("1500"))
                .mandorWagePerKg(bd("2000"))
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigRepository.findFirstBy()).thenReturn(Optional.of(existing));
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(bd("1200"));
        request.setSupirTrukWagePerKg(bd("1700"));
        request.setMandorWagePerKg(bd("2200"));

        WageConfigResponse result = wageConfigService.updateWageConfig(request);

        assertThat(result.getBuruhWagePerKg()).isEqualByComparingTo("1200.00");
        assertThat(result.getSupirTrukWagePerKg()).isEqualByComparingTo("1700.00");
        assertThat(result.getMandorWagePerKg()).isEqualByComparingTo("2200.00");
    }

    @Test
    void updateConfig_negativeValue_shouldThrow() {
        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(bd("-100"));
        request.setSupirTrukWagePerKg(bd("1500"));
        request.setMandorWagePerKg(bd("2000"));

        assertThatThrownBy(() -> wageConfigService.updateWageConfig(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
