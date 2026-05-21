package com.mysawit.pembayaran.grpc;

import java.math.BigDecimal;
import java.util.UUID;

final class GrpcRequestParsers {

    private GrpcRequestParsers() {
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    static UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID", ex);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(fieldName + " must not be null", ex);
        }
    }

    static BigDecimal parseDecimal(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid decimal", ex);
        }
    }
}
