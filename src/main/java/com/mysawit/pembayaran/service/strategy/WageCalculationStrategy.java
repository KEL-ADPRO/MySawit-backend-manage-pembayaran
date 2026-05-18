package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.model.enums.UserRole;

public interface WageCalculationStrategy {
    double getWagePerKg(WageConfig config);
    double calculate(double wagePerKg, double kilogram);
    UserRole getSupportedRole();
}
