package com.mysawit.pembayaran.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectPayrollRequest {

    @NotBlank
    private String rejectionReason;
}
