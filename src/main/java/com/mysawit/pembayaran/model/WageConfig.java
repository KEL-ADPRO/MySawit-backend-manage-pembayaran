package com.mysawit.pembayaran.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wage_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal buruhWagePerKg;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal supirTrukWagePerKg;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal mandorWagePerKg;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
