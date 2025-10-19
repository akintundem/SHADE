package ai.eventplanner.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String category;

    private String priceRange;

    private Double rating;

    private String googlePlaceId;
}

