package com.mysawit.pembayaran.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private double buruhWagePerKg;

    @Column(nullable = false)
    private double supirTrukWagePerKg;

    @Column(nullable = false)
    private double mandorWagePerKg;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
