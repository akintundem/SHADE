package ai.eventplanner.vendor.dto.response;

import lombok.Data;

@Data
public class VendorResponse {
    
    private String id;
    private String name;
    private String category;
    private String priceRange;
    private Double rating;
    private String googlePlaceId;
}
