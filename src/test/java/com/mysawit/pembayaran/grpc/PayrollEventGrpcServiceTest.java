package com.mysawit.pembayaran.grpc;

import com.mysawit.pembayaran.dto.request.HarvestPayrollEventRequest;
import com.mysawit.pembayaran.dto.response.PayrollResponse;
import com.mysawit.pembayaran.grpc.proto.HarvestApprovalPayrollEventRequest;
import com.mysawit.pembayaran.grpc.proto.PayrollEventResponse;
import com.mysawit.pembayaran.model.enums.PayrollKilogramType;
import com.mysawit.pembayaran.model.enums.PayrollSourceType;
import com.mysawit.pembayaran.model.enums.PayrollStatus;
import com.mysawit.pembayaran.model.enums.UserRole;
import com.mysawit.pembayaran.service.PayrollIntegrationService;
import io.grpc.Status;
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
class PayrollEventGrpcServiceTest {

    @Mock
    private PayrollIntegrationService payrollIntegrationService;

    private PayrollEventGrpcService service;

    @BeforeEach
    void setUp() {
        service = new PayrollEventGrpcService(payrollIntegrationService);
    }

    @Test
    void createFromHarvestApprovalMapsRequestAndResponse() {
        UUID payrollId = UUID.fromString("8f0d9ca2-87ce-4c70-905d-64ffbf4bfc2f");
        UUID userId = UUID.fromString("57b7895e-103f-4a51-8e32-041fb0ca2509");
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 20, 10, 30);
        PayrollResponse payroll = PayrollResponse.builder()
                .id(payrollId)
                .userId(userId)
                .userRole(UserRole.BURUH)
                .amount(new BigDecimal("45000.00"))
                .kilogram(new BigDecimal("100.500"))
                .harvestedKg(new BigDecimal("100.500"))
                .kilogramType(PayrollKilogramType.HARVESTED)
                .sourceType(PayrollSourceType.HARVEST_APPROVAL)
                .sourceId("harvest-1")
                .idempotencyKey("harvest-1:buruh")
                .description("Payroll BURUH")
                .status(PayrollStatus.PENDING)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        when(payrollIntegrationService.createFromHarvestApproval(any()))
                .thenReturn(CompletableFuture.completedFuture(payroll));

        HarvestApprovalPayrollEventRequest request = HarvestApprovalPayrollEventRequest.newBuilder()
                .setSourceId("harvest-1")
                .setBuruhUserId(userId.toString())
                .setHarvestedKg("100.500")
                .setIdempotencyKey("harvest-1:buruh")
                .build();
        CapturingStreamObserver<PayrollEventResponse> observer = new CapturingStreamObserver<>();

        service.createFromHarvestApproval(request, observer);

        ArgumentCaptor<HarvestPayrollEventRequest> captor =
                ArgumentCaptor.forClass(HarvestPayrollEventRequest.class);
        verify(payrollIntegrationService).createFromHarvestApproval(captor.capture());
        assertThat(captor.getValue().getSourceId()).isEqualTo("harvest-1");
        assertThat(captor.getValue().getBuruhUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getHarvestedKg()).isEqualByComparingTo("100.500");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("harvest-1:buruh");

        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
        assertThat(observer.value.getId()).isEqualTo(payrollId.toString());
        assertThat(observer.value.getUserRole()).isEqualTo("BURUH");
        assertThat(observer.value.getAmount()).isEqualTo("45000.00");
        assertThat(observer.value.getHarvestedKg()).isEqualTo("100.500");
        assertThat(observer.value.getStatus()).isEqualTo("PENDING");
        assertThat(observer.value.getCreatedAt()).isEqualTo("2026-05-20T10:30");
    }

    @Test
    void createFromHarvestApprovalRejectsInvalidUuid() {
        HarvestApprovalPayrollEventRequest request = HarvestApprovalPayrollEventRequest.newBuilder()
                .setSourceId("harvest-1")
                .setBuruhUserId("not-a-uuid")
                .setHarvestedKg("100.500")
                .setIdempotencyKey("harvest-1:buruh")
                .build();
        CapturingStreamObserver<PayrollEventResponse> observer = new CapturingStreamObserver<>();

        service.createFromHarvestApproval(request, observer);

        verifyNoInteractions(payrollIntegrationService);
        assertThat(Status.fromThrowable(observer.error).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(Status.fromThrowable(observer.error).getDescription()).contains("buruh_user_id");
        assertThat(observer.completed).isFalse();
    }

}
