package com.mysawit.pembayaran.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysawit.pembayaran.client.PaymentInvoice;
import com.mysawit.pembayaran.client.XenditClient;
import com.mysawit.pembayaran.repository.PayrollRepository;
import com.mysawit.pembayaran.repository.TopUpTransactionRepository;
import com.mysawit.pembayaran.repository.WageConfigRepository;
import com.mysawit.pembayaran.repository.WalletRepository;
import com.mysawit.pembayaran.service.PayrollServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "xendit.callback-token=functional-token",
                "xendit.exchange-rate=10000",
                "xendit.amount-step=10000",
                "spring.jpa.show-sql=false"
        }
)
class PaymentFlowFunctionalTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private TopUpTransactionRepository topUpTransactionRepository;

    @Autowired
    private WageConfigRepository wageConfigRepository;

    @MockitoBean
    private XenditClient xenditClient;

    @BeforeEach
    void setUp() {
        payrollRepository.deleteAll();
        topUpTransactionRepository.deleteAll();
        walletRepository.deleteAll();
        wageConfigRepository.deleteAll();

        reset(xenditClient);
        when(xenditClient.createInvoice(anyString(), anyDouble(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String externalId = invocation.getArgument(0);
                    return new PaymentInvoice(externalId, "https://checkout.test/invoices/" + externalId);
                });
    }

    @Test
    void adminTopUpAndPayrollApprovalMoveBalancesAcrossWallets() {
        UUID adminId = PayrollServiceImpl.ADMIN_USER_ID;
        UUID workerId = UUID.randomUUID();

        createWallet(adminId);
        createWallet(workerId);
        updateWageConfig(2.0, 3.0, 4.0);

        JsonNode topUp = initiateTopUp(adminId, 1_000_000.0);
        assertThat(topUp.path("status").asText()).isEqualTo("PENDING");
        assertThat(topUp.path("amountSawitDollar").asDouble()).isEqualTo(100.0);

        postWithHeaders(
                "/api/pembayaran/wallet/topup/callback",
                Map.of("external_id", topUp.path("paymentGatewayRef").asText(), "status", "PAID"),
                Map.of("x-callback-token", "functional-token"),
                HttpStatus.OK
        );
        assertWalletBalance(adminId, 100.0);

        JsonNode payroll = createPayroll(workerId, "BURUH", 40.0);
        assertThat(payroll.path("status").asText()).isEqualTo("PENDING");
        assertThat(payroll.path("amount").asDouble()).isEqualTo(72.0);

        JsonNode approvedPayroll = approvePayroll(payroll.path("id").asText());
        assertThat(approvedPayroll.path("status").asText()).isEqualTo("ACCEPTED");

        assertWalletBalance(adminId, 28.0);
        assertWalletBalance(workerId, 72.0);

        ResponseEntity<JsonNode> acceptedPayrolls = get(
                "/api/pembayaran/payroll?status=ACCEPTED&userId=" + workerId,
                HttpStatus.OK
        );
        assertThat(acceptedPayrolls.getBody()).isNotNull();
        assertThat(acceptedPayrolls.getBody().isArray()).isTrue();
        assertThat(acceptedPayrolls.getBody().size()).isEqualTo(1);
        assertThat(acceptedPayrolls.getBody().get(0).path("id").asText())
                .isEqualTo(payroll.path("id").asText());
    }

    @Test
    void protectedEndpointsRejectMissingOrInvalidHeaders() {
        get("/api/pembayaran/wallet/me", HttpStatus.UNAUTHORIZED);

        putWithHeaders(
                "/api/pembayaran/wage-config",
                Map.of("buruhWagePerKg", 2.0, "supirTrukWagePerKg", 3.0, "mandorWagePerKg", 4.0),
                Map.of("X-User-Role", "WORKER"),
                HttpStatus.FORBIDDEN
        );

        postWithHeaders(
                "/api/pembayaran/wallet/topup",
                Map.of("userId", UUID.randomUUID(), "amountRupiah", 100_000.0),
                Map.of("X-User-Role", "WORKER"),
                HttpStatus.FORBIDDEN
        );

        postWithHeaders(
                "/api/pembayaran/wallet/topup/callback",
                Map.of("external_id", "missing", "status", "PAID"),
                Map.of("x-callback-token", "wrong-token"),
                HttpStatus.UNAUTHORIZED
        );

        verifyNoInteractions(xenditClient);
    }

    @Test
    void validationErrorsReturnBadRequestBeforeStateChanges() {
        JsonNode invalidTopUp = postWithHeaders(
                "/api/pembayaran/wallet/topup",
                Map.of("userId", UUID.randomUUID(), "amountRupiah", 15_000.0),
                Map.of("X-User-Role", "ADMIN"),
                HttpStatus.BAD_REQUEST
        );
        assertThat(invalidTopUp.path("status").asInt()).isEqualTo(400);
        assertThat(topupCount()).isZero();

        JsonNode invalidPayroll = postWithHeaders(
                "/api/pembayaran/payroll",
                Map.of("userId", UUID.randomUUID(), "userRole", "BURUH", "kilogram", -1.0),
                Map.of("X-User-Id", UUID.randomUUID().toString()),
                HttpStatus.BAD_REQUEST
        );
        assertThat(invalidPayroll.path("error").asText()).isEqualTo("Validation failed");
        assertThat(payrollRepository.count()).isZero();
    }

    private void createWallet(UUID userId) {
        JsonNode wallet = postWithHeaders(
                "/api/pembayaran/wallet",
                Map.of(),
                Map.of("X-User-Id", userId.toString()),
                HttpStatus.CREATED
        );
        assertThat(wallet).isNotNull();
        assertThat(wallet.path("userId").asText()).isEqualTo(userId.toString());
        assertThat(wallet.path("balance").asDouble()).isZero();
    }

    private void updateWageConfig(double buruhWage, double supirTrukWage, double mandorWage) {
        JsonNode config = putWithHeaders(
                "/api/pembayaran/wage-config",
                Map.of(
                        "buruhWagePerKg", buruhWage,
                        "supirTrukWagePerKg", supirTrukWage,
                        "mandorWagePerKg", mandorWage
                ),
                Map.of("X-User-Role", "ADMIN"),
                HttpStatus.OK
        );
        assertThat(config.path("buruhWagePerKg").asDouble()).isEqualTo(buruhWage);
    }

    private JsonNode initiateTopUp(UUID userId, double amountRupiah) {
        return postWithHeaders(
                "/api/pembayaran/wallet/topup",
                Map.of("userId", userId, "amountRupiah", amountRupiah),
                Map.of("X-User-Role", "ADMIN"),
                HttpStatus.CREATED
        );
    }

    private JsonNode createPayroll(UUID workerId, String role, double kilogram) {
        return postWithHeaders(
                "/api/pembayaran/payroll",
                Map.of("userId", workerId, "userRole", role, "kilogram", kilogram),
                Map.of("X-User-Id", UUID.randomUUID().toString()),
                HttpStatus.CREATED
        );
    }

    private JsonNode approvePayroll(String payrollId) {
        return putWithHeaders(
                "/api/pembayaran/payroll/" + payrollId + "/approve",
                Map.of(),
                Map.of("X-User-Role", "ADMIN"),
                HttpStatus.OK
        );
    }

    private void assertWalletBalance(UUID userId, double expectedBalance) {
        ResponseEntity<JsonNode> response = get("/api/pembayaran/wallet/" + userId, HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("balance").asDouble()).isEqualTo(expectedBalance);
    }

    private ResponseEntity<JsonNode> get(String path, HttpStatus expectedStatus) {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return response;
    }

    private JsonNode postWithHeaders(
            String path,
            Map<String, ?> body,
            Map<String, String> headers,
            HttpStatus expectedStatus
    ) {
        ResponseEntity<JsonNode> response = exchange(path, HttpMethod.POST, body, headers);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return response.getBody();
    }

    private JsonNode putWithHeaders(
            String path,
            Map<String, ?> body,
            Map<String, String> headers,
            HttpStatus expectedStatus
    ) {
        ResponseEntity<JsonNode> response = exchange(path, HttpMethod.PUT, body, headers);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return response.getBody();
    }

    private ResponseEntity<JsonNode> exchange(
            String path,
            HttpMethod method,
            Map<String, ?> body,
            Map<String, String> headerValues
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headerValues.forEach(headers::set);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private long topupCount() {
        return topUpTransactionRepository.count();
    }
}
