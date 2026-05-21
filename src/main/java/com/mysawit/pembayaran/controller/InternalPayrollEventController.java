package com.mysawit.pembayaran.controller;

import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.HarvestPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.service.PayrollIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/internal/payroll-events")
@RequiredArgsConstructor
public class InternalPayrollEventController {

    private final PayrollIntegrationService payrollIntegrationService;

    @PostMapping("/harvest-approved")
    public CompletableFuture<ResponseEntity<PayrollResponse>> createFromHarvestApproval(
            @Valid @RequestBody HarvestPayrollEventRequest request) {
        return payrollIntegrationService.createFromHarvestApproval(request)
                .thenApply(payroll -> ResponseEntity.status(HttpStatus.CREATED).body(payroll));
    }

    @PostMapping("/driver-delivery-approved")
    public CompletableFuture<ResponseEntity<PayrollResponse>> createFromDriverDeliveryApproval(
            @Valid @RequestBody DriverDeliveryPayrollEventRequest request) {
        return payrollIntegrationService.createFromDriverDeliveryApproval(request)
                .thenApply(payroll -> ResponseEntity.status(HttpStatus.CREATED).body(payroll));
    }

    @PostMapping("/factory-delivery-approved")
    public CompletableFuture<ResponseEntity<PayrollResponse>> createFromFactoryDeliveryApproval(
            @Valid @RequestBody FactoryDeliveryPayrollEventRequest request) {
        return payrollIntegrationService.createFromFactoryDeliveryApproval(request)
                .thenApply(payroll -> ResponseEntity.status(HttpStatus.CREATED).body(payroll));
    }
}
