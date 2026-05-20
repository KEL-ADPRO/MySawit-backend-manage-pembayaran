package com.mysawit.pembayaran.grpc;

import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.HarvestPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.exception.InsufficientBalanceException;
import com.mysawit.pembayaran.exception.PayrollNotFoundException;
import com.mysawit.pembayaran.grpc.proto.DriverDeliveryApprovalPayrollEventRequest;
import com.mysawit.pembayaran.grpc.proto.FactoryDeliveryApprovalPayrollEventRequest;
import com.mysawit.pembayaran.grpc.proto.HarvestApprovalPayrollEventRequest;
import com.mysawit.pembayaran.grpc.proto.PayrollEventResponse;
import com.mysawit.pembayaran.grpc.proto.PayrollEventServiceGrpc;
import com.mysawit.pembayaran.service.PayrollIntegrationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@GrpcService
@RequiredArgsConstructor
public class PayrollEventGrpcService extends PayrollEventServiceGrpc.PayrollEventServiceImplBase {

    private final PayrollIntegrationService payrollIntegrationService;

    @Override
    public void createFromHarvestApproval(HarvestApprovalPayrollEventRequest request,
                                          StreamObserver<PayrollEventResponse> responseObserver) {
        try {
            CompletableFuture<PayrollResponse> payroll =
                    payrollIntegrationService.createFromHarvestApproval(toHarvestDto(request));
            completeWith(payroll, responseObserver);
        } catch (RuntimeException ex) {
            responseObserver.onError(toStatus(ex).asRuntimeException());
        }
    }

    @Override
    public void createFromDriverDeliveryApproval(DriverDeliveryApprovalPayrollEventRequest request,
                                                 StreamObserver<PayrollEventResponse> responseObserver) {
        try {
            CompletableFuture<PayrollResponse> payroll =
                    payrollIntegrationService.createFromDriverDeliveryApproval(toDriverDeliveryDto(request));
            completeWith(payroll, responseObserver);
        } catch (RuntimeException ex) {
            responseObserver.onError(toStatus(ex).asRuntimeException());
        }
    }

    @Override
    public void createFromFactoryDeliveryApproval(FactoryDeliveryApprovalPayrollEventRequest request,
                                                  StreamObserver<PayrollEventResponse> responseObserver) {
        try {
            CompletableFuture<PayrollResponse> payroll =
                    payrollIntegrationService.createFromFactoryDeliveryApproval(toFactoryDeliveryDto(request));
            completeWith(payroll, responseObserver);
        } catch (RuntimeException ex) {
            responseObserver.onError(toStatus(ex).asRuntimeException());
        }
    }

    private void completeWith(CompletableFuture<PayrollResponse> payroll,
                              StreamObserver<PayrollEventResponse> responseObserver) {
        payroll.whenComplete((response, throwable) -> {
            if (throwable != null) {
                responseObserver.onError(toStatus(throwable).asRuntimeException());
                return;
            }
            responseObserver.onNext(toProto(response));
            responseObserver.onCompleted();
        });
    }

    private HarvestPayrollEventRequest toHarvestDto(HarvestApprovalPayrollEventRequest request) {
        HarvestPayrollEventRequest dto = new HarvestPayrollEventRequest();
        dto.setSourceId(request.getSourceId());
        dto.setBuruhUserId(parseUuid(request.getBuruhUserId(), "buruh_user_id"));
        dto.setHarvestedKg(parseDecimal(request.getHarvestedKg(), "harvested_kg"));
        dto.setIdempotencyKey(request.getIdempotencyKey());
        return dto;
    }

    private DriverDeliveryPayrollEventRequest toDriverDeliveryDto(DriverDeliveryApprovalPayrollEventRequest request) {
        DriverDeliveryPayrollEventRequest dto = new DriverDeliveryPayrollEventRequest();
        dto.setSourceId(request.getSourceId());
        dto.setSupirUserId(parseUuid(request.getSupirUserId(), "supir_user_id"));
        dto.setDeliveredKg(parseDecimal(request.getDeliveredKg(), "delivered_kg"));
        dto.setIdempotencyKey(request.getIdempotencyKey());
        return dto;
    }

    private FactoryDeliveryPayrollEventRequest toFactoryDeliveryDto(FactoryDeliveryApprovalPayrollEventRequest request) {
        FactoryDeliveryPayrollEventRequest dto = new FactoryDeliveryPayrollEventRequest();
        dto.setSourceId(request.getSourceId());
        dto.setMandorUserId(parseUuid(request.getMandorUserId(), "mandor_user_id"));
        dto.setRecognizedKg(parseDecimal(request.getRecognizedKg(), "recognized_kg"));
        dto.setIdempotencyKey(request.getIdempotencyKey());
        return dto;
    }

    private static UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid UUID", ex);
        }
    }

    private static BigDecimal parseDecimal(String value, String fieldName) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid decimal", ex);
        }
    }

    private static PayrollEventResponse toProto(PayrollResponse response) {
        return PayrollEventResponse.newBuilder()
                .setId(stringValue(response.getId()))
                .setUserId(stringValue(response.getUserId()))
                .setUserRole(enumValue(response.getUserRole()))
                .setAmount(decimalValue(response.getAmount()))
                .setKilogram(decimalValue(response.getKilogram()))
                .setHarvestedKg(decimalValue(response.getHarvestedKg()))
                .setDeliveredKg(decimalValue(response.getDeliveredKg()))
                .setRecognizedKg(decimalValue(response.getRecognizedKg()))
                .setKilogramType(enumValue(response.getKilogramType()))
                .setSourceType(enumValue(response.getSourceType()))
                .setSourceId(stringValue(response.getSourceId()))
                .setIdempotencyKey(stringValue(response.getIdempotencyKey()))
                .setDescription(stringValue(response.getDescription()))
                .setStatus(enumValue(response.getStatus()))
                .setRejectionReason(stringValue(response.getRejectionReason()))
                .setCreatedAt(dateTimeValue(response.getCreatedAt()))
                .setUpdatedAt(dateTimeValue(response.getUpdatedAt()))
                .build();
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

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String decimalValue(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static String enumValue(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private static String dateTimeValue(LocalDateTime value) {
        return value == null ? "" : value.toString();
    }
}
