package com.mysawit.mysawit_pembayaran.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollRateResponse {
    private String id;
    private BigDecimal workerRatePerKg;
    private BigDecimal driverRatePerKg;
    private BigDecimal foremanRatePerKg;
}