package com.mysawit.mysawit_pembayaran.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String userId) {
        super("Wallet not found for user: " + userId);
    }
}