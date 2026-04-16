package com.mysawit.pembayaran.dto.response;

import com.mysawit.pembayaran.model.enums.TopUpStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TopUpResponse {

    private UUID id;
    private UUID userId;
    private double amountRupiah;
    private double amountSawitDollar;
    private String paymentGatewayRef;
    private String paymentUrl;
    private TopUpStatus status;
    private LocalDateTime createdAt;
}
