package com.mysawit.pembayaran.grpc;

import com.mysawit.integration.payment.proto.TriggerPayrollRequest;
import com.mysawit.integration.payment.proto.TriggerPayrollResponse;
import com.mysawit.pembayaran.dto.request.DriverDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.request.FactoryDeliveryPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.model.enums.PayrollKilogramType;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.service.PayrollIntegrationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGrpcServiceImplTest {

    @Mock
    private PayrollIntegrationService payrollIntegrationService;

    private PaymentGrpcServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentGrpcServiceImpl(payrollIntegrationService);
    }

    @Test
    void triggerPayrollForSupirCallsDriverDeliveryApproval() {
        UUID actorId = UUID.fromString("57b7895e-103f-4a51-8e32-041fb0ca2509");
        String shipmentId = "shipment-1";
        PayrollResponse payroll = payrollResponse(UserRole.SUPIR_TRUK, PayrollSourceType.DRIVER_DELIVERY_APPROVAL);
        when(payrollIntegrationService.createFromDriverDeliveryApproval(any()))
                .thenReturn(CompletableFuture.completedFuture(payroll));

        TriggerPayrollRequest request = TriggerPayrollRequest.newBuilder()
                .setActorId(actorId.toString())
                .setShipmentId(shipmentId)
                .setRole("SUPIR")
                .setWeightKg("150.250")
                .build();
        CapturingObserver<TriggerPayrollResponse> observer = new CapturingObserver<>();

        service.triggerPayroll(request, observer);

        ArgumentCaptor<DriverDeliveryPayrollEventRequest> captor =
                ArgumentCaptor.forClass(DriverDeliveryPayrollEventRequest.class);
        verify(payrollIntegrationService).createFromDriverDeliveryApproval(captor.capture());
        assertThat(captor.getValue().getSourceId()).isEqualTo(shipmentId);
        assertThat(captor.getValue().getSupirUserId()).isEqualTo(actorId);
        assertThat(captor.getValue().getDeliveredKg()).isEqualByComparingTo("150.250");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("shipment:shipment-1:role:SUPIR");

        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
        assertThat(observer.value.getAccepted()).isTrue();
    }

    @Test
    void triggerPayrollForMandorCallsFactoryDeliveryApproval() {
        UUID actorId = UUID.fromString("8f0d9ca2-87ce-4c70-905d-64ffbf4bfc2f");
        String shipmentId = "shipment-9";
        PayrollResponse payroll = payrollResponse(UserRole.MANDOR, PayrollSourceType.FACTORY_DELIVERY_APPROVAL);
        when(payrollIntegrationService.createFromFactoryDeliveryApproval(any()))
                .thenReturn(CompletableFuture.completedFuture(payroll));

        TriggerPayrollRequest request = TriggerPayrollRequest.newBuilder()
                .setActorId(actorId.toString())
                .setShipmentId(shipmentId)
                .setRole("MANDOR")
                .setWeightKg("380.000")
                .build();
        CapturingObserver<TriggerPayrollResponse> observer = new CapturingObserver<>();

        service.triggerPayroll(request, observer);

        ArgumentCaptor<FactoryDeliveryPayrollEventRequest> captor =
                ArgumentCaptor.forClass(FactoryDeliveryPayrollEventRequest.class);
        verify(payrollIntegrationService).createFromFactoryDeliveryApproval(captor.capture());
        assertThat(captor.getValue().getSourceId()).isEqualTo(shipmentId);
        assertThat(captor.getValue().getMandorUserId()).isEqualTo(actorId);
        assertThat(captor.getValue().getRecognizedKg()).isEqualByComparingTo("380.000");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("shipment:shipment-9:role:MANDOR");

        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
        assertThat(observer.value.getAccepted()).isTrue();
    }

    @Test
    void triggerPayrollRejectsUnknownRole() {
        TriggerPayrollRequest request = TriggerPayrollRequest.newBuilder()
                .setActorId(UUID.randomUUID().toString())
                .setShipmentId("shipment-1")
                .setRole("BURUH")
                .setWeightKg("100")
                .build();
        CapturingObserver<TriggerPayrollResponse> observer = new CapturingObserver<>();

        service.triggerPayroll(request, observer);

        verifyNoInteractions(payrollIntegrationService);
        assertThat(Status.fromThrowable(observer.error).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(observer.completed).isFalse();
    }

    @Test
    void triggerPayrollRejectsInvalidUuid() {
        TriggerPayrollRequest request = TriggerPayrollRequest.newBuilder()
                .setActorId("not-a-uuid")
                .setShipmentId("shipment-1")
                .setRole("SUPIR")
                .setWeightKg("100")
                .build();
        CapturingObserver<TriggerPayrollResponse> observer = new CapturingObserver<>();

        service.triggerPayroll(request, observer);

        verifyNoInteractions(payrollIntegrationService);
        assertThat(Status.fromThrowable(observer.error).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(Status.fromThrowable(observer.error).getDescription()).contains("actor_id");
    }

    @Test
    void triggerPayrollRejectsInvalidWeight() {
        TriggerPayrollRequest request = TriggerPayrollRequest.newBuilder()
                .setActorId(UUID.randomUUID().toString())
                .setShipmentId("shipment-1")
                .setRole("MANDOR")
                .setWeightKg("not-a-number")
                .build();
        CapturingObserver<TriggerPayrollResponse> observer = new CapturingObserver<>();

        service.triggerPayroll(request, observer);

        verifyNoInteractions(payrollIntegrationService);
        assertThat(Status.fromThrowable(observer.error).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(Status.fromThrowable(observer.error).getDescription()).contains("weight_kg");
    }

    @Test
    void triggerPayrollRejectsBlankShipmentId() {
        TriggerPayrollRequest request = TriggerPayrollRequest.newBuilder()
                .setActorId(UUID.randomUUID().toString())
                .setShipmentId("")
                .setRole("SUPIR")
                .setWeightKg("100")
                .build();
        CapturingObserver<TriggerPayrollResponse> observer = new CapturingObserver<>();

        service.triggerPayroll(request, observer);

        verifyNoInteractions(payrollIntegrationService);
        assertThat(Status.fromThrowable(observer.error).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(Status.fromThrowable(observer.error).getDescription()).contains("shipment_id");
    }

    private static PayrollResponse payrollResponse(UserRole role, PayrollSourceType sourceType) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 21, 10, 0);
        return PayrollResponse.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .userRole(role)
                .amount(new BigDecimal("50000.00"))
                .kilogram(new BigDecimal("100.000"))
                .kilogramType(PayrollKilogramType.DELIVERED)
                .sourceType(sourceType)
                .sourceId("shipment-1")
                .idempotencyKey("ignored")
                .description("Test payroll")
                .status(PayrollStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static final class CapturingObserver<T> implements StreamObserver<T> {
        private T value;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable throwable) {
            this.error = throwable;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }
}
