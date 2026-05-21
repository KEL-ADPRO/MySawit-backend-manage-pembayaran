package com.mysawit.pembayaran.grpc;

import com.mysawit.integration.payment.proto.PaymentGrpcServiceGrpc;
import com.mysawit.integration.payment.proto.TriggerPayrollRequest;
import com.mysawit.integration.payment.proto.TriggerPayrollResponse;
import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.service.PayrollIntegrationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@GrpcService
@RequiredArgsConstructor
public class PaymentGrpcServiceImpl extends PaymentGrpcServiceGrpc.PaymentGrpcServiceImplBase {

    private static final String ROLE_SUPIR = "SUPIR";
    private static final String ROLE_MANDOR = "MANDOR";

    private final PayrollIntegrationService payrollIntegrationService;

    @Override
    public void triggerPayroll(TriggerPayrollRequest request,
                               StreamObserver<TriggerPayrollResponse> responseObserver) {
        try {
            String role = request.getRole();
            String shipmentId = requireText(request.getShipmentId(), "shipment_id");
            UUID actorId = parseUuid(request.getActorId(), "actor_id");
            BigDecimal weightKg = parseDecimal(request.getWeightKg(), "weight_kg");
            String idempotencyKey = "shipment:" + shipmentId + ":role:" + role;

            CompletableFuture<PayrollResponse> payroll = dispatch(role, shipmentId, actorId, weightKg, idempotencyKey);
            completeWith(payroll, responseObserver);
        } catch (RuntimeException ex) {
            responseObserver.onError(toStatus(ex).asRuntimeException());
        }
    }

    private CompletableFuture<PayrollResponse> dispatch(String role, String shipmentId, UUID actorId,
                                                       BigDecimal weightKg, String idempotencyKey) {
        if (ROLE_SUPIR.equals(role)) {
            DriverDeliveryPayrollEventRequest dto = new DriverDeliveryPayrollEventRequest();
            dto.setSourceId(shipmentId);
            dto.setSupirUserId(actorId);
            dto.setDeliveredKg(weightKg);
            dto.setIdempotencyKey(idempotencyKey);
            return payrollIntegrationService.createFromDriverDeliveryApproval(dto);
        }
        if (ROLE_MANDOR.equals(role)) {
            FactoryDeliveryPayrollEventRequest dto = new FactoryDeliveryPayrollEventRequest();
            dto.setSourceId(shipmentId);
            dto.setMandorUserId(actorId);
            dto.setRecognizedKg(weightKg);
            dto.setIdempotencyKey(idempotencyKey);
            return payrollIntegrationService.createFromFactoryDeliveryApproval(dto);
        }
        throw new IllegalArgumentException("role must be SUPIR or MANDOR");
    }

    private void completeWith(CompletableFuture<PayrollResponse> payroll,
                              StreamObserver<TriggerPayrollResponse> responseObserver) {
        payroll.whenComplete((response, throwable) -> {
            if (throwable != null) {
                responseObserver.onError(toStatus(throwable).asRuntimeException());
                return;
            }
            responseObserver.onNext(TriggerPayrollResponse.newBuilder().setAccepted(true).build());
            responseObserver.onCompleted();
        });
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID", ex);
        } catch (NullPointerException ex) {
            throw new IllegalArgumentException(fieldName + " must not be null", ex);
        }
    }

    private static BigDecimal parseDecimal(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid decimal", ex);
        }
    }

    private static Status toStatus(Throwable throwable) {
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
