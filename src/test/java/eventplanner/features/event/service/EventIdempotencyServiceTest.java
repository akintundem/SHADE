package eventplanner.features.event.service;

import eventplanner.features.event.dto.response.CreateEventWithCoverUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Black-box tests that validate the public idempotency contract using Redis stubs.
 */
@ExtendWith(MockitoExtension.class)
class EventIdempotencyServiceTest {
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private EventIdempotencyService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new EventIdempotencyService(redisTemplate);

        // Lenient defaults keep the tests resilient to minor implementation variations.
        lenient().when(valueOperations.setIfAbsent(anyString(), any())).thenReturn(Boolean.FALSE);
        lenient().when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Boolean.FALSE);
    }

    @Test
    void markAsProcessing_returnsTrue_whenLockIsAcquired() {
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Boolean.TRUE);

        boolean acquired = service.markAsProcessing("event:create:123");

        assertTrue(acquired);
    }

    @Test
    void markAsProcessing_returnsFalse_whenLockIsAlreadyHeld() {
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(Boolean.FALSE);

        boolean acquired = service.markAsProcessing("event:create:123");

        assertFalse(acquired);
    }

    @Test
    void getProcessedResult_returnsOptional_whenValueExists() {
        CreateEventWithCoverUploadResponse response = CreateEventWithCoverUploadResponse.builder().build();
        when(valueOperations.get(anyString())).thenReturn(response);

        Optional<CreateEventWithCoverUploadResponse> result = service.getProcessedResult("event:create:123");

        assertTrue(result.isPresent());
        assertSame(response, result.get());
    }
}
