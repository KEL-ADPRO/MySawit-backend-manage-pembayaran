package com.mysawit.pembayaran.model;

import com.mysawit.pembayaran.model.enums.TopUpStatus;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private double amountRupiah;

    @Column(nullable = false)
    private double amountSawitDollar;

    private String paymentGatewayRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopUpStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
