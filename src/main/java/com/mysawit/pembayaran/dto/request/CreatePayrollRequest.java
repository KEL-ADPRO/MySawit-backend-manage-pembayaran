package com.mysawit.pembayaran.dto.request;

import com.mysawit.pembayaran.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePayrollRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UserRole userRole;

    @Positive
    private double kilogram;

    private String description;
}
