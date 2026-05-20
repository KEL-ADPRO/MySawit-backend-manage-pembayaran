package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.enums.UserRole;

import java.math.BigDecimal;

public interface WageCalculationStrategy {
    BigDecimal calculate(BigDecimal wagePerKg, BigDecimal kilogram);
    UserRole getSupportedRole();
}
