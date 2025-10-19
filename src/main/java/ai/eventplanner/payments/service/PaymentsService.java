package ai.eventplanner.payments.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentsService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentsService.class);

    private final String apiKey;

    public PaymentsService(@Value("${STRIPE_API_KEY:${stripe.api.key:}}") String apiKey) {
        this.apiKey = apiKey;
        if (StringUtils.hasText(apiKey)) {
            Stripe.apiKey = apiKey;
            logger.info("Stripe API key configured; real payment intents will be created.");
        } else {
            logger.warn("Stripe API key not configured. Falling back to local-only payment intents.");
        }
    }

    public Map<String, Object> createPlatformFeeIntent(Long amountCents, String currency, String userId) throws StripeException {
        if (!StringUtils.hasText(apiKey)) {
            return buildLocalIntent(amountCents, currency, userId);
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setDescription("Event Planner Platform Fee")
                .putMetadata("user_id", userId == null ? "" : userId)
                .build();
        PaymentIntent intent = PaymentIntent.create(params);
        return Map.of("clientSecret", intent.getClientSecret(), "id", intent.getId());
    }

    private Map<String, Object> buildLocalIntent(Long amountCents, String currency, String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", "pi_local_" + UUID.randomUUID());
        response.put("clientSecret", "secret_local_" + UUID.randomUUID());
        response.put("amount", amountCents);
        response.put("currency", currency);
        response.put("status", "requires_confirmation");
        response.put("livemode", false);
        response.put("createdAt", OffsetDateTime.now().toString());
        response.put("metadata", Map.of("user_id", userId == null ? "" : userId, "mode", "local"));
        return response;
    }
}

