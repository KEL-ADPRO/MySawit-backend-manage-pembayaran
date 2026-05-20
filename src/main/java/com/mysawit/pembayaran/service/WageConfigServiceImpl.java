package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.UpdateWageConfigRequest;
import com.mysawit.pembayaran.dto.response.WageConfigResponse;
import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WageConfigServiceImpl implements WageConfigService {

    private final WageConfigRepository wageConfigRepository;
    private static final int MONEY_SCALE = 2;

    @Override
    public WageConfigResponse getWageConfig() {
        WageConfig config = wageConfigRepository.findFirstBy()
                .orElseGet(() -> {
                    WageConfig defaultConfig = WageConfig.builder()
                            .buruhWagePerKg(normalizeMoney(BigDecimal.ZERO))
                            .supirTrukWagePerKg(normalizeMoney(BigDecimal.ZERO))
                            .mandorWagePerKg(normalizeMoney(BigDecimal.ZERO))
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return wageConfigRepository.save(defaultConfig);
                });
        return toResponse(config);
    }

    @Override
    public WageConfigResponse updateWageConfig(UpdateWageConfigRequest request) {
        BigDecimal buruhWage = normalizeNonNegativeWage(request.getBuruhWagePerKg(), "buruhWagePerKg");
        BigDecimal supirWage = normalizeNonNegativeWage(request.getSupirTrukWagePerKg(), "supirTrukWagePerKg");
        BigDecimal mandorWage = normalizeNonNegativeWage(request.getMandorWagePerKg(), "mandorWagePerKg");

        WageConfig config = wageConfigRepository.findFirstBy()
                .orElseGet(() -> WageConfig.builder()
                        .buruhWagePerKg(normalizeMoney(BigDecimal.ZERO))
                        .supirTrukWagePerKg(normalizeMoney(BigDecimal.ZERO))
                        .mandorWagePerKg(normalizeMoney(BigDecimal.ZERO))
                        .updatedAt(LocalDateTime.now())
                        .build());
        config.setBuruhWagePerKg(buruhWage);
        config.setSupirTrukWagePerKg(supirWage);
        config.setMandorWagePerKg(mandorWage);
        config.setUpdatedAt(LocalDateTime.now());
        return toResponse(wageConfigRepository.save(config));
    }

    private BigDecimal normalizeNonNegativeWage(BigDecimal wage, String fieldName) {
        if (wage == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (wage.signum() < 0) {
            throw new IllegalArgumentException("Wage values cannot be negative");
        }
        return normalizeMoney(wage);
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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
