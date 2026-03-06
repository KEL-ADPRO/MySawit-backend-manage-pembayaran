package com.mysawit.mysawit_pembayaran.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Insufficient admin balance");
    }
}