package com.mysawit.pembayaran.grpc;

import com.mysawit.integration.payment.proto.PaymentGrpcServiceGrpc;
import com.mysawit.integration.payment.proto.TriggerPayrollRequest;
import com.mysawit.integration.payment.proto.TriggerPayrollResponse;
import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.service.PayrollIntegrationService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
            String shipmentId = GrpcRequestParsers.requireText(request.getShipmentId(), "shipment_id");
            UUID actorId = GrpcRequestParsers.parseUuid(request.getActorId(), "actor_id");
            BigDecimal weightKg = GrpcRequestParsers.parseDecimal(request.getWeightKg(), "weight_kg");
            String idempotencyKey = "shipment:" + shipmentId + ":role:" + role;

            CompletableFuture<PayrollResponse> payroll = dispatch(role, shipmentId, actorId, weightKg, idempotencyKey);
            payroll.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    responseObserver.onError(GrpcStatusMapper.toStatus(throwable).asRuntimeException());
                    return;
                }
                responseObserver.onNext(TriggerPayrollResponse.newBuilder().setAccepted(true).build());
                responseObserver.onCompleted();
            });
        } catch (RuntimeException ex) {
            responseObserver.onError(GrpcStatusMapper.toStatus(ex).asRuntimeException());
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
}
