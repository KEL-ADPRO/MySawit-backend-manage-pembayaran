package com.mysawit.pembayaran.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WageConfigResponse {

    private UUID id;
    private BigDecimal buruhWagePerKg;
    private BigDecimal supirTrukWagePerKg;
    private BigDecimal mandorWagePerKg;
    private LocalDateTime updatedAt;
}
