package eventplanner.features.budget.util;

import eventplanner.features.budget.dto.response.BudgetDetailResponse;
import eventplanner.features.budget.entity.Budget;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for Budget HTTP response handling (ETags, Caching)
 */
public class BudgetHttpUtil {

    private BudgetHttpUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generate ETag from budget entity version and updated timestamp
     */
    public static String generateETag(Budget budget) {
        if (budget == null) return "";
        long version = budget.getVersion() != null ? budget.getVersion() : 0L;
        LocalDateTime timestamp = budget.getUpdatedAt() != null ? budget.getUpdatedAt() : budget.getCreatedAt();
        long millis = timestamp != null ? timestamp.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli() : 0L;
        return "\"" + version + "-" + millis + "\"";
    }

    /**
     * Wrap response with ETag and Cache-Control headers
     */
    public static ResponseEntity<BudgetDetailResponse> wrapResponse(Budget budget, BudgetDetailResponse response) {
        if (budget == null || response == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .eTag(generateETag(budget))
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).mustRevalidate().cachePrivate())
                .lastModified(budget.getUpdatedAt() != null ? 
                        budget.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant() :
                        budget.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
                .body(response);
    }
}
