package com.mysawit.pembayaran.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateWageConfigRequest {

    @Positive
    private double buruhWagePerKg;

    @Positive
    private double supirTrukWagePerKg;

    @Positive
    private double mandorWagePerKg;
}
