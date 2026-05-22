package com.mysawit.pembayaran.grpc;

import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.HarvestPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.grpc.proto.DriverDeliveryApprovalPayrollEventRequest;
import com.mysawit.pembayaran.grpc.proto.FactoryDeliveryApprovalPayrollEventRequest;
import com.mysawit.pembayaran.grpc.proto.HarvestApprovalPayrollEventRequest;
import com.mysawit.pembayaran.grpc.proto.PayrollEventResponse;
import com.mysawit.pembayaran.grpc.proto.PayrollEventServiceGrpc;
import com.mysawit.pembayaran.service.PayrollIntegrationService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

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
            responseObserver.onError(GrpcStatusMapper.toStatus(ex).asRuntimeException());
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
            responseObserver.onError(GrpcStatusMapper.toStatus(ex).asRuntimeException());
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
            responseObserver.onError(GrpcStatusMapper.toStatus(ex).asRuntimeException());
        }
    }

    private void completeWith(CompletableFuture<PayrollResponse> payroll,
                              StreamObserver<PayrollEventResponse> responseObserver) {
        payroll.whenComplete((response, throwable) -> {
            if (throwable != null) {
                responseObserver.onError(GrpcStatusMapper.toStatus(throwable).asRuntimeException());
                return;
            }
            responseObserver.onNext(toProto(response));
            responseObserver.onCompleted();
        });
    }

    private HarvestPayrollEventRequest toHarvestDto(HarvestApprovalPayrollEventRequest request) {
        HarvestPayrollEventRequest dto = new HarvestPayrollEventRequest();
        dto.setSourceId(request.getSourceId());
        dto.setBuruhUserId(GrpcRequestParsers.parseUuid(request.getBuruhUserId(), "buruh_user_id"));
        dto.setHarvestedKg(GrpcRequestParsers.parseDecimal(request.getHarvestedKg(), "harvested_kg"));
        dto.setIdempotencyKey(request.getIdempotencyKey());
        return dto;
    }

    private DriverDeliveryPayrollEventRequest toDriverDeliveryDto(DriverDeliveryApprovalPayrollEventRequest request) {
        DriverDeliveryPayrollEventRequest dto = new DriverDeliveryPayrollEventRequest();
        dto.setSourceId(request.getSourceId());
        dto.setSupirUserId(GrpcRequestParsers.parseUuid(request.getSupirUserId(), "supir_user_id"));
        dto.setDeliveredKg(GrpcRequestParsers.parseDecimal(request.getDeliveredKg(), "delivered_kg"));
        dto.setIdempotencyKey(request.getIdempotencyKey());
        return dto;
    }

    private FactoryDeliveryPayrollEventRequest toFactoryDeliveryDto(FactoryDeliveryApprovalPayrollEventRequest request) {
        FactoryDeliveryPayrollEventRequest dto = new FactoryDeliveryPayrollEventRequest();
        dto.setSourceId(request.getSourceId());
        dto.setMandorUserId(GrpcRequestParsers.parseUuid(request.getMandorUserId(), "mandor_user_id"));
        dto.setRecognizedKg(GrpcRequestParsers.parseDecimal(request.getRecognizedKg(), "recognized_kg"));
        dto.setIdempotencyKey(request.getIdempotencyKey());
        return dto;
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
