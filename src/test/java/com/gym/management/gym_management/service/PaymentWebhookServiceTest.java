package com.gym.management.gym_management.service;

import com.gym.management.gym_management.dto.PaymentWebhookRequest;
import com.gym.management.gym_management.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {
    private static final String SECRET = "test-webhook-secret-not-for-production";

    @Mock
    private PaymentService paymentService;

    @Test
    void verifiesSignatureBeforeCompletingTheTenantScopedPayment() throws Exception {
        PaymentWebhookService service = new PaymentWebhookService(paymentService, SECRET);
        PaymentWebhookRequest request = new PaymentWebhookRequest(7L, 42L, "provider-ref-42");

        service.processCompletedPayment(request, signature(request));

        verify(paymentService).completeProviderPayment(7L, 42L, "provider-ref-42");
    }

    @Test
    void rejectsInvalidSignatureWithoutTouchingPayment() {
        PaymentWebhookService service = new PaymentWebhookService(paymentService, SECRET);
        PaymentWebhookRequest request = new PaymentWebhookRequest(7L, 42L, "provider-ref-42");

        assertThrows(InvalidCredentialsException.class,
                () -> service.processCompletedPayment(request, "00".repeat(32)));

        verifyNoInteractions(paymentService);
    }

    private String signature(PaymentWebhookRequest request) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String payload = request.organizationId() + ":" + request.paymentId() + ":"
                + request.providerReference();
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
