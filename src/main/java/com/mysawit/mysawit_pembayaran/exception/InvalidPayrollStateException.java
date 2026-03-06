package com.mysawit.mysawit_pembayaran.exception;

public class InvalidPayrollStateException extends RuntimeException {
    public InvalidPayrollStateException(String message) {
        super(message);
    }
}