package com.mysawit.pembayaran.dto.response;

import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PayrollResponse {

    private UUID id;
    private UUID userId;
    private UserRole userRole;
    private double amount;
    private double kilogram;
    private String description;
    private PayrollStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
