package com.mysawit.mysawit_pembayaran.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payroll_rate_configs")
public class PayrollRateConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private BigDecimal workerRatePerKg = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal driverRatePerKg = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal foremanRatePerKg = BigDecimal.ZERO;
}