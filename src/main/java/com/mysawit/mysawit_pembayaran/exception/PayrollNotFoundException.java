package com.mysawit.mysawit_pembayaran.exception;

public class PayrollNotFoundException extends RuntimeException {
    public PayrollNotFoundException(String payrollId) {
        super("Payroll not found: " + payrollId);
    }
}