package com.mysawit.mysawit_pembayaran.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentTransactionResponse {
    private String id;
    private String walletId;
    private BigDecimal amount;
    private String type;
    private String status;
    private String externalReference;
    private LocalDateTime createdAt;
}