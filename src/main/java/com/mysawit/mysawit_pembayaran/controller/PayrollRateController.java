package com.mysawit.mysawit_pembayaran.controller;

import com.mysawit.mysawit_pembayaran.dto.request.UpdatePayrollRateRequest;
import com.mysawit.mysawit_pembayaran.dto.response.PayrollRateResponse;
import com.mysawit.mysawit_pembayaran.service.PayrollRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payroll-rates")
@RequiredArgsConstructor
public class PayrollRateController {

    private final PayrollRateService payrollRateService;

    @GetMapping
    public PayrollRateResponse getPayrollRate() {
        return payrollRateService.getPayrollRate();
    }

    @PutMapping
    public PayrollRateResponse updatePayrollRate(@Valid @RequestBody UpdatePayrollRateRequest request) {
        return payrollRateService.updatePayrollRate(request);
    }
}