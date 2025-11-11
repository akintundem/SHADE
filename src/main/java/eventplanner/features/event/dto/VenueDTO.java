package eventplanner.features.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Venue information containing location and venue details
 */
@Schema(description = "Venue information with location details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueDTO {

    @Schema(description = "Street address")
    private String address;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State/Province")
    private String state;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "Zip/Postal code")
    private String zipCode;

    @Schema(description = "Latitude coordinate")
    private BigDecimal latitude;

    @Schema(description = "Longitude coordinate")
    private BigDecimal longitude;

    @Schema(description = "Google Place ID")
    private String googlePlaceId;

    @Schema(description = "Additional Google Place data (JSON)")
    private String googlePlaceData;
}

