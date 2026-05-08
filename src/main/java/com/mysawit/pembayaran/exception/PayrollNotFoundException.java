package com.mysawit.pembayaran.exception;

import java.util.UUID;

public class PayrollNotFoundException extends RuntimeException {

    public PayrollNotFoundException(UUID id) {
        super("Payroll not found with id: " + id);
    }
}
