package com.mysawit.pembayaran.model;

import com.mysawit.pembayaran.model.enums.TopUpStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "topup_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountRupiah;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountSawitDollar;

    @Column(nullable = false, unique = true)
    private String paymentGatewayRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopUpStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}
