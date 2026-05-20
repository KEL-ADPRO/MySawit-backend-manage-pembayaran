package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.enums.UserRole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MandorWageStrategy implements WageCalculationStrategy {

    private static final BigDecimal PAYROLL_MULTIPLIER = new BigDecimal("0.90");

    @Override
    public BigDecimal calculate(BigDecimal wagePerKg, BigDecimal kilogram) {
        return wagePerKg.multiply(kilogram).multiply(PAYROLL_MULTIPLIER);
    }

    @Override
    public UserRole getSupportedRole() {
        return UserRole.MANDOR;
    }
}
