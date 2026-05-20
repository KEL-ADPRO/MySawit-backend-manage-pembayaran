package com.mysawit.pembayaran.service;

import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.HarvestPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;

import java.util.concurrent.CompletableFuture;

public interface PayrollIntegrationService {

    CompletableFuture<PayrollResponse> createFromHarvestApproval(HarvestPayrollEventRequest request);

    CompletableFuture<PayrollResponse> createFromDriverDeliveryApproval(DriverDeliveryPayrollEventRequest request);

    CompletableFuture<PayrollResponse> createFromFactoryDeliveryApproval(FactoryDeliveryPayrollEventRequest request);
}
