package eventplanner.common.communication.services.resilient;

import eventplanner.common.communication.exception.NotificationFailureException;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resilient wrapper around NotificationService with:
 * - Automatic retry with exponential backoff
 * - Circuit breaker pattern for fast-fail
 * - Metrics collection for monitoring
 * - Dead letter queue for permanent failures
 *
 * This service adds operational resilience without changing the
 * underlying NotificationService behavior.
 */
@Service
@Slf4j
public class ResilientNotificationService {

    private final NotificationService notificationService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter notificationSuccessCounter;
    private final Counter notificationFailureCounter;
    private final Counter notificationRetryCounter;
    private final Counter circuitBreakerOpenCounter;

    public ResilientNotificationService(
            NotificationService notificationService,
            DeadLetterQueueService deadLetterQueueService,
            MeterRegistry meterRegistry) {
        this.notificationService = notificationService;
        this.deadLetterQueueService = deadLetterQueueService;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.notificationSuccessCounter = Counter.builder("notification.success")
                .description("Total successful notifications sent")
                .register(meterRegistry);

        this.notificationFailureCounter = Counter.builder("notification.failure")
                .description("Total failed notifications")
                .register(meterRegistry);

        this.notificationRetryCounter = Counter.builder("notification.retry")
                .description("Total notification retry attempts")
                .register(meterRegistry);

        this.circuitBreakerOpenCounter = Counter.builder("notification.circuit.breaker.open")
                .description("Circuit breaker opened count")
                .register(meterRegistry);
    }

    /**
     * Send notification with retry and circuit breaker protection.
     * Falls back to dead letter queue if all retries fail.
     *
     * @param request Notification request
     * @return NotificationResponse
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendFallback")
    @Retry(name = "notificationService")
    public NotificationResponse send(NotificationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            NotificationResponse response = notificationService.send(request);

            sample.stop(Timer.builder("notification.send")
                    .tag("type", request.getType() != null ? request.getType().toString() : "UNKNOWN")
                    .tag("success", String.valueOf(response.isSuccess()))
                    .register(meterRegistry));

            if (response.isSuccess()) {
                notificationSuccessCounter.increment();
            } else {
                notificationFailureCounter.increment();
                logFailure(request, response.getErrorMessage());
            }

            return response;

        } catch (Exception e) {
            sample.stop(Timer.builder("notification.send")
                    .tag("type", request.getType() != null ? request.getType().toString() : "UNKNOWN")
                    .tag("success", "false")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));

            notificationFailureCounter.increment();
            throw e;
        }
    }

    /**
     * Send notification with strict failure handling (throws exception on failure).
     * Includes retry and circuit breaker protection.
     *
     * @param request Notification request
     * @return NotificationResponse only if successful
     * @throws NotificationFailureException if notification fails after retries
     */
    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendOrThrowFallback")
    @Retry(name = "notificationService")
    public NotificationResponse sendOrThrow(NotificationRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            NotificationResponse response = notificationService.sendOrThrow(request);

            sample.stop(Timer.builder("notification.send.strict")
                    .tag("type", request.getType() != null ? request.getType().toString() : "UNKNOWN")
                    .tag("success", "true")
                    .register(meterRegistry));

            notificationSuccessCounter.increment();
            return response;

        } catch (NotificationFailureException e) {
            sample.stop(Timer.builder("notification.send.strict")
                    .tag("type", request.getType() != null ? request.getType().toString() : "UNKNOWN")
                    .tag("success", "false")
                    .tag("error", "NotificationFailureException")
                    .register(meterRegistry));

            notificationFailureCounter.increment();
            logFailure(request, e.getMessage());

            // Add to dead letter queue for later retry
            deadLetterQueueService.addFailedNotification(request, e.getMessage());

            throw e;

        } catch (Exception e) {
            sample.stop(Timer.builder("notification.send.strict")
                    .tag("type", request.getType() != null ? request.getType().toString() : "UNKNOWN")
                    .tag("success", "false")
                    .tag("error", e.getClass().getSimpleName())
                    .register(meterRegistry));

            notificationFailureCounter.increment();
            throw e;
        }
    }

    /**
     * Fallback method for send() when circuit breaker is open.
     * Returns failure response and queues notification for later retry.
     */
    private NotificationResponse sendFallback(NotificationRequest request, Exception e) {
        log.warn("Circuit breaker OPEN for notification service. Fallback triggered for: {}",
                request.getType(), e);

        circuitBreakerOpenCounter.increment();

        // Queue for later retry
        deadLetterQueueService.addFailedNotification(request,
                "Circuit breaker open: " + e.getMessage());

        return NotificationResponse.failure(
                null,
                "Service temporarily unavailable. Notification queued for retry.",
                null
        );
    }

    /**
     * Fallback method for sendOrThrow() when circuit breaker is open.
     * Throws exception to maintain strict contract.
     */
    private NotificationResponse sendOrThrowFallback(NotificationRequest request, Exception e) {
        log.error("Circuit breaker OPEN for notification service. sendOrThrow fallback triggered for: {}",
                request.getType(), e);

        circuitBreakerOpenCounter.increment();

        // Queue for later retry
        deadLetterQueueService.addFailedNotification(request,
                "Circuit breaker open: " + e.getMessage());

        throw new NotificationFailureException(
                "Notification service unavailable (circuit breaker open). Operation rolled back.",
                null,
                request.getType() != null ? request.getType().toString() : "UNKNOWN",
                e
        );
    }

    private void logFailure(NotificationRequest request, String errorMessage) {
        log.error("Notification failed - Type: {}, To: {}, Error: {}",
                request.getType(),
                maskSensitiveData(request.getTo()),
                errorMessage);
    }

    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }

    /**
     * Record a retry attempt (called by Resilience4j)
     */
    public void recordRetry() {
        notificationRetryCounter.increment();
    }
}
