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

    @Test
    void getConfig_exists_shouldReturn() {
        WageConfig config = WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(1000.0)
                .supirTrukWagePerKg(1500.0)
                .mandorWagePerKg(2000.0)
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(config));

        WageConfigResponse result = wageConfigService.getWageConfig();

        assertThat(result.getBuruhWagePerKg()).isEqualTo(1000.0);
        assertThat(result.getSupirTrukWagePerKg()).isEqualTo(1500.0);
        assertThat(result.getMandorWagePerKg()).isEqualTo(2000.0);
    }

    @Test
    void getConfig_notExists_shouldAutoCreateDefault() {
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WageConfigResponse result = wageConfigService.getWageConfig();

        assertThat(result.getBuruhWagePerKg()).isEqualTo(0.0);
        assertThat(result.getSupirTrukWagePerKg()).isEqualTo(0.0);
        assertThat(result.getMandorWagePerKg()).isEqualTo(0.0);
        verify(wageConfigRepository).save(any(WageConfig.class));
    }

    @Test
    void updateConfig_success() {
        WageConfig existing = WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(1000.0)
                .supirTrukWagePerKg(1500.0)
                .mandorWagePerKg(2000.0)
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(1200.0);
        request.setSupirTrukWagePerKg(1700.0);
        request.setMandorWagePerKg(2200.0);

        WageConfigResponse result = wageConfigService.updateWageConfig(request);

        assertThat(result.getBuruhWagePerKg()).isEqualTo(1200.0);
        assertThat(result.getSupirTrukWagePerKg()).isEqualTo(1700.0);
        assertThat(result.getMandorWagePerKg()).isEqualTo(2200.0);
    }

    @Test
    void updateConfig_negativeValue_shouldThrow() {
        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(-100.0);
        request.setSupirTrukWagePerKg(1500.0);
        request.setMandorWagePerKg(2000.0);

        assertThatThrownBy(() -> wageConfigService.updateWageConfig(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getConfig_shouldDelegateToLatestUpdatedRow() {
        WageConfig latest = WageConfig.builder()
                .id(UUID.randomUUID())
                .buruhWagePerKg(7777.0)
                .supirTrukWagePerKg(8888.0)
                .mandorWagePerKg(9999.0)
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(latest));

        WageConfigResponse result = wageConfigService.getWageConfig();

        verify(wageConfigRepository).findTopByOrderByUpdatedAtDesc();
        assertThat(result.getBuruhWagePerKg()).isEqualTo(7777.0);
        assertThat(result.getSupirTrukWagePerKg()).isEqualTo(8888.0);
        assertThat(result.getMandorWagePerKg()).isEqualTo(9999.0);
    }

    @Test
    void updateConfig_shouldUpdateLatestRowInPlace() {
        UUID latestId = UUID.randomUUID();
        WageConfig latest = WageConfig.builder()
                .id(latestId)
                .buruhWagePerKg(1000.0)
                .supirTrukWagePerKg(1500.0)
                .mandorWagePerKg(2000.0)
                .updatedAt(LocalDateTime.now())
                .build();
        when(wageConfigRepository.findTopByOrderByUpdatedAtDesc()).thenReturn(Optional.of(latest));
        when(wageConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateWageConfigRequest request = new UpdateWageConfigRequest();
        request.setBuruhWagePerKg(5000.0);
        request.setSupirTrukWagePerKg(6000.0);
        request.setMandorWagePerKg(7000.0);

        WageConfigResponse result = wageConfigService.updateWageConfig(request);

        assertThat(result.getId()).isEqualTo(latestId);
        assertThat(result.getBuruhWagePerKg()).isEqualTo(5000.0);
        assertThat(result.getSupirTrukWagePerKg()).isEqualTo(6000.0);
        assertThat(result.getMandorWagePerKg()).isEqualTo(7000.0);
    }
}
