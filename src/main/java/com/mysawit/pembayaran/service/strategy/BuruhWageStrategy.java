package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.model.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class BuruhWageStrategy implements WageCalculationStrategy {

    @Override
    public double getWagePerKg(WageConfig config) {
        return config.getBuruhWagePerKg();
    }

    @Override
    public double calculate(double wagePerKg, double kilogram) {
        return wagePerKg * kilogram * 0.9;
    }

    @Override
    public UserRole getSupportedRole() {
        return UserRole.BURUH;
    }
}
