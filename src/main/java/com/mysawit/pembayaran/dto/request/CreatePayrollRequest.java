package com.mysawit.pembayaran.dto.request;

import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreatePayrollRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UserRole userRole;

    private PayrollSourceType sourceType;

    private String sourceId;

    private String idempotencyKey;

    private BigDecimal kilogram;

    private BigDecimal harvestedKg;

    private BigDecimal deliveredKg;

    private BigDecimal recognizedKg;

    private String description;
}
