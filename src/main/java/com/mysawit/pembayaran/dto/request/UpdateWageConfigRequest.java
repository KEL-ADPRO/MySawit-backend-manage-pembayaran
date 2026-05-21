package com.mysawit.pembayaran.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateWageConfigRequest {

    @PositiveOrZero
    private BigDecimal buruhWagePerKg;

    @PositiveOrZero
    private BigDecimal supirTrukWagePerKg;

    @PositiveOrZero
    private BigDecimal mandorWagePerKg;
}
