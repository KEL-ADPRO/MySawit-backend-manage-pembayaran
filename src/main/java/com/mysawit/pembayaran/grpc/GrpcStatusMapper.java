package com.mysawit.pembayaran.grpc;

import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import io.grpc.Status;

import java.util.concurrent.CompletionException;

final class GrpcStatusMapper {

    private GrpcStatusMapper() {
    }

    static Status toStatus(Throwable throwable) {
        Throwable root = unwrap(throwable);
        String message = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();

        if (root instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(message).withCause(root);
        }
        if (root instanceof PayrollNotFoundException) {
            return Status.NOT_FOUND.withDescription(message).withCause(root);
        }
        if (root instanceof InsufficientBalanceException || root instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription(message).withCause(root);
        }
        return Status.INTERNAL.withDescription("An unexpected error occurred").withCause(root);
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return unwrap(throwable.getCause());
        }
        return throwable;
    }
}
