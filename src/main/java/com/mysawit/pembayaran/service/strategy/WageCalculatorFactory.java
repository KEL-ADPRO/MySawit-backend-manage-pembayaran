package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.WageConfig;
import com.mysawit.pembayaran.model.enums.UserRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WageCalculatorFactory {

    private final Map<UserRole, WageCalculationStrategy> strategies;

    public WageCalculatorFactory(List<WageCalculationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(WageCalculationStrategy::getSupportedRole, s -> s));
    }

    public double calculate(UserRole role, double wagePerKg, double kilogram) {
        return getStrategy(role).calculate(wagePerKg, kilogram);
    }

    public double getWagePerKg(UserRole role, WageConfig config) {
        return getStrategy(role).getWagePerKg(config);
    }

    private WageCalculationStrategy getStrategy(UserRole role) {
        WageCalculationStrategy strategy = strategies.get(role);
        if (strategy == null) {
            throw new IllegalArgumentException("No wage strategy found for role: " + role);
        }
        return strategy;
    }
}
