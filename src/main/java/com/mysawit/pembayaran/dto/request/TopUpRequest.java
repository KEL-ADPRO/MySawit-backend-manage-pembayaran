package com.mysawit.pembayaran.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class TopUpRequest {

    private UUID userId;

    @Positive
    private double amountRupiah;
}
