package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class MandorWageStrategy implements WageCalculationStrategy {

    @Override
    public double calculate(double wagePerKg, double kilogram) {
        return wagePerKg * kilogram * 0.9;
    }

    @Override
    public UserRole getSupportedRole() {
        return UserRole.MANDOR;
    }
}
