package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class BuruhWageStrategy implements WageCalculationStrategy {

    @Override
    public double calculate(double wagePerKg, double kilogram) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public UserRole getSupportedRole() {
        return UserRole.BURUH;
    }
}
