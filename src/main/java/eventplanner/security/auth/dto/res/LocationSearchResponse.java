package eventplanner.security.auth.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for location search results with coordinates and UUID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchResponse {
    private UUID id;
    private String city;
    private String state;
    private String country;
    private String displayName; // e.g., "New York, NY, United States"
    private BigDecimal latitude;
    private BigDecimal longitude;
}

