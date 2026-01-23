package eventplanner.common.communication.config;

import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener for Resilience4j events.
 * Logs circuit breaker state changes and retry attempts for operational visibility.
 */
@Component
@Slf4j
public class ResilienceEventListener {

    @EventListener
    public void onCircuitBreakerStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.warn("Circuit Breaker '{}' changed state from {} to {}",
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState(),
                event.getStateTransition().getToState());

        if (event.getStateTransition().getToState().name().equals("OPEN")) {
            log.error("ALERT: Circuit breaker '{}' is now OPEN. External service is failing!",
                    event.getCircuitBreakerName());
            // TODO: Send alert to monitoring system (PagerDuty, Slack, etc.)
        } else if (event.getStateTransition().getToState().name().equals("HALF_OPEN")) {
            log.info("Circuit breaker '{}' is HALF_OPEN. Testing if service recovered.",
                    event.getCircuitBreakerName());
        } else if (event.getStateTransition().getToState().name().equals("CLOSED")) {
            log.info("Circuit breaker '{}' is CLOSED. Service recovered successfully.",
                    event.getCircuitBreakerName());
        }
    }

    @EventListener
    public void onCircuitBreakerError(CircuitBreakerOnErrorEvent event) {
        log.debug("Circuit breaker '{}' recorded error: {}",
                event.getCircuitBreakerName(),
                event.getThrowable().getMessage());
    }

    @EventListener
    public void onRetry(RetryOnRetryEvent event) {
        log.info("Retry attempt #{} for operation '{}' after {}ms. Last error: {}",
                event.getNumberOfRetryAttempts(),
                event.getName(),
                event.getWaitInterval().toMillis(),
                event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "unknown");
    }
}
