package com.mysawit.mysawit_pembayaran.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopUpWalletRequest {

    @NotBlank
    private String userId;

    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}