package com.mysawit.pembayaran.model;

import com.mysawit.pembayaran.model.enums.PayrollKilogramType;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payrolls", indexes = {
        @Index(name = "idx_payrolls_user_status", columnList = "user_id,status"),
        @Index(name = "idx_payrolls_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal kilogram;

    @Column(precision = 19, scale = 3)
    private BigDecimal harvestedKg;

    @Column(precision = 19, scale = 3)
    private BigDecimal deliveredKg;

    @Column(precision = 19, scale = 3)
    private BigDecimal recognizedKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollKilogramType kilogramType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollSourceType sourceType;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollStatus status;

    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
