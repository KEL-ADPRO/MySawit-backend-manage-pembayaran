package com.mysawit.pembayaran.dto.response;

import com.mysawit.pembayaran.model.enums.PayrollKilogramType;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PayrollResponse {

    private UUID id;
    private UUID userId;
    private UserRole userRole;
    private BigDecimal amount;
    private BigDecimal kilogram;
    private BigDecimal harvestedKg;
    private BigDecimal deliveredKg;
    private BigDecimal recognizedKg;
    private PayrollKilogramType kilogramType;
    private PayrollSourceType sourceType;
    private String sourceId;
    private String idempotencyKey;
    private String description;
    private PayrollStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
