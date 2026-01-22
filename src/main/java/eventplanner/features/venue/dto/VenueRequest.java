package eventplanner.features.venue.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a venue
 */
@Data
public class VenueRequest {

    @NotBlank(message = "Venue name is required")
    @Size(max = 255, message = "Venue name must not exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Min(value = 0, message = "Capacity must be >= 0")
    private Integer capacity;

    @Size(max = 50, message = "Venue type must not exceed 50 characters")
    private String venueType;

    private String accessibilityFeatures;

    private String description;

    @Size(max = 40, message = "Phone must not exceed 40 characters")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 180, message = "Email must not exceed 180 characters")
    private String email;

    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    private String websiteUrl;

    private Boolean parkingAvailable;

    private Boolean publicTransitNearby;
}
