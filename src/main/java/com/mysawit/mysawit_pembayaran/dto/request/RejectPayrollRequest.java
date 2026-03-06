package com.mysawit.mysawit_pembayaran.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectPayrollRequest {

    @NotBlank
    private String reason;
}