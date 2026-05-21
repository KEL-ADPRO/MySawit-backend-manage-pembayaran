package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.service.strategy.BuruhWageStrategy;
import com.mysawit.pembayaran.service.strategy.MandorWageStrategy;
import com.mysawit.pembayaran.service.strategy.SupirTrukWageStrategy;
import com.mysawit.pembayaran.service.strategy.WageCalculationStrategy;
import com.mysawit.pembayaran.service.strategy.WageCalculatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class WageCalculatorFactoryTest {

    private WageCalculatorFactory factory;

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

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
        BigDecimal result = factory.calculate(UserRole.BURUH, bd("5000"), bd("100"));
        assertThat(result).isEqualByComparingTo("450000");
    }

    @Test
    void calculate_supirTruk_shouldReturnCorrectAmount() {
        BigDecimal result = factory.calculate(UserRole.SUPIR_TRUK, bd("3000"), bd("200"));
        assertThat(result).isEqualByComparingTo("540000");
    }

    @Test
    void calculate_mandor_shouldReturnCorrectAmount() {
        BigDecimal result = factory.calculate(UserRole.MANDOR, bd("4000"), bd("150"));
        assertThat(result).isEqualByComparingTo("540000");
    }

    @Test
    void calculate_decimalPrecision_shouldNotUseDoubleMath() {
        BigDecimal result = factory.calculate(UserRole.BURUH, bd("0.10"), bd("3"));
        assertThat(result).isEqualByComparingTo("0.2700");
    }
}
