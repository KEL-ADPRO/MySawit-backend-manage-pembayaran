package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WageConfigServiceImpl implements WageConfigService {

    private final WageConfigRepository wageConfigRepository;

    @Override
    @Transactional
    public WageConfigResponse getWageConfig() {
        WageConfig config = wageConfigRepository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(() -> {
                    WageConfig defaultConfig = WageConfig.builder()
                            .buruhWagePerKg(0.0)
                            .supirTrukWagePerKg(0.0)
                            .mandorWagePerKg(0.0)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return wageConfigRepository.save(defaultConfig);
                });
        return toResponse(config);
    }

    @Override
    @Transactional
    public WageConfigResponse updateWageConfig(UpdateWageConfigRequest request) {
        if (request.getBuruhWagePerKg() < 0
                || request.getSupirTrukWagePerKg() < 0
                || request.getMandorWagePerKg() < 0) {
            throw new IllegalArgumentException("Wage values cannot be negative");
        }
        WageConfig config = wageConfigRepository.findTopByOrderByUpdatedAtDesc()
                .orElseGet(WageConfig::new);
        config.setBuruhWagePerKg(request.getBuruhWagePerKg());
        config.setSupirTrukWagePerKg(request.getSupirTrukWagePerKg());
        config.setMandorWagePerKg(request.getMandorWagePerKg());
        config.setUpdatedAt(LocalDateTime.now());
        return toResponse(wageConfigRepository.save(config));
    }

    private WageConfigResponse toResponse(WageConfig config) {
        return WageConfigResponse.builder()
                .id(config.getId())
                .buruhWagePerKg(config.getBuruhWagePerKg())
                .supirTrukWagePerKg(config.getSupirTrukWagePerKg())
                .mandorWagePerKg(config.getMandorWagePerKg())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
