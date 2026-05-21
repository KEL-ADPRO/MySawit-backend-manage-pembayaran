package com.mysawit.pembayaran.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DriverDeliveryPayrollEventRequest {

    @NotBlank
    private String sourceId;

    @NotNull
    private UUID supirUserId;

    @NotNull
    @Positive
    private BigDecimal deliveredKg;

    @NotBlank
    private String idempotencyKey;
}
