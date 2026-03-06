package com.mysawit.mysawit_pembayaran.dto.response;

import com.mysawit.mysawit_pembayaran.model.PayrollStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollResponse {
    private String id;
    private String recipientUserId;
    private String recipientRole;
    private BigDecimal amount;
    private String description;
    private PayrollStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}