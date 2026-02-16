package eventplanner.features.venue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for venue data.
 * {@code distanceMeters} is populated only for proximity-based searches.
 */
@Data
public class VenueResponse {

    private UUID id;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer capacity;
    private String venueType;
    private String accessibilityFeatures;
    private String description;
    private String phone;
    private String email;
    private String websiteUrl;
    private Boolean parkingAvailable;
    private Boolean publicTransitNearby;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Distance from search point in metres — only present for proximity queries. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double distanceMeters;

    /**
     * Get full address as a single string
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (addressLine1 != null) {
            address.append(addressLine1);
        }
        if (addressLine2 != null && !addressLine2.isBlank()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine2);
        }
        if (city != null) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }
        if (state != null) {
            if (address.length() > 0) address.append(", ");
            address.append(state);
        }
        if (postalCode != null) {
            if (address.length() > 0) address.append(" ");
            address.append(postalCode);
        }
        if (country != null) {
            if (address.length() > 0) address.append(", ");
            address.append(country);
        }
        return address.toString();
    }
}
