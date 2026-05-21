package com.mysawit.pembayaran.service.strategy;

import com.mysawit.pembayaran.model.enums.UserRole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    public BigDecimal calculate(UserRole role, BigDecimal wagePerKg, BigDecimal kilogram) {
        WageCalculationStrategy strategy = strategies.get(role);
        if (strategy == null) {
            throw new IllegalArgumentException("No wage strategy found for role: " + role);
        }
        return strategy.calculate(wagePerKg, kilogram);
    }
}
