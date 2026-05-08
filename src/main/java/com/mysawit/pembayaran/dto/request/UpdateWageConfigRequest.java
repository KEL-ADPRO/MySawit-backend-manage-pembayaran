package com.mysawit.pembayaran.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class UpdateWageConfigRequest {

    @PositiveOrZero
    private double buruhWagePerKg;

    @PositiveOrZero
    private double supirTrukWagePerKg;

    @PositiveOrZero
    private double mandorWagePerKg;
}
