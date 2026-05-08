package com.mysawit.pembayaran.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WageConfigResponse {

    private UUID id;
    private double buruhWagePerKg;
    private double supirTrukWagePerKg;
    private double mandorWagePerKg;
    private LocalDateTime updatedAt;
}
