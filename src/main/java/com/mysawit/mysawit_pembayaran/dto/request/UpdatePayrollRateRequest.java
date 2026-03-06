package com.mysawit.mysawit_pembayaran.dto.request;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePayrollRateRequest {

    @DecimalMin(value = "0.00")
    private BigDecimal workerRatePerKg;

    @DecimalMin(value = "0.00")
    private BigDecimal driverRatePerKg;

    @DecimalMin(value = "0.00")
    private BigDecimal foremanRatePerKg;
}