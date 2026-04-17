package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.service.strategy.BuruhWageStrategy;
import com.mysawit.pembayaran.service.strategy.MandorWageStrategy;
import com.mysawit.pembayaran.service.strategy.SupirTrukWageStrategy;
import com.mysawit.pembayaran.service.strategy.WageCalculationStrategy;
import com.mysawit.pembayaran.service.strategy.WageCalculatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class WageCalculatorFactoryTest {

    private WageCalculatorFactory factory;

    @BeforeEach
    void setUp() {
        List<WageCalculationStrategy> strategies = List.of(
                new BuruhWageStrategy(),
                new SupirTrukWageStrategy(),
                new MandorWageStrategy()
        );
        factory = new WageCalculatorFactory(strategies);
    }

    @Test
    void calculate_buruh_shouldReturnCorrectAmount() {
        double result = factory.calculate(UserRole.BURUH, 5000.0, 100.0);
        assertThat(result).isEqualTo(5000.0 * 100.0 * 0.9);
    }

    @Test
    void calculate_supirTruk_shouldReturnCorrectAmount() {
        double result = factory.calculate(UserRole.SUPIR_TRUK, 3000.0, 200.0);
        assertThat(result).isEqualTo(3000.0 * 200.0 * 0.9);
    }

    @Test
    void calculate_mandor_shouldReturnCorrectAmount() {
        double result = factory.calculate(UserRole.MANDOR, 4000.0, 150.0);
        assertThat(result).isEqualTo(4000.0 * 150.0 * 0.9);
    }

    @Test
    void calculate_zeroKilogram_shouldReturnZero() {
        double result = factory.calculate(UserRole.BURUH, 5000.0, 0.0);
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void calculate_shouldUseDifferentStrategyPerRole() {
        double buruhResult = factory.calculate(UserRole.BURUH, 1000.0, 10.0);
        double supirResult = factory.calculate(UserRole.SUPIR_TRUK, 1000.0, 10.0);
        double mandorResult = factory.calculate(UserRole.MANDOR, 1000.0, 10.0);

        assertThat(buruhResult).isEqualTo(9000.0);
        assertThat(supirResult).isEqualTo(9000.0);
        assertThat(mandorResult).isEqualTo(9000.0);

        assertThat(buruhResult).isEqualTo(supirResult).isEqualTo(mandorResult);
    }
}
