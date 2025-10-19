package ai.eventplanner.vendor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for searching vendors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to search for vendors")
public class SearchVendorRequest {
    
    @Schema(description = "Search query string", example = "wedding photographer")
    private String q;
    
    @Schema(description = "Vendor category filter", example = "photography")
    private String category;
    
    @Schema(description = "Price range filter", example = "$$")
    private String priceRange;
    
    @Schema(description = "Minimum rating filter", example = "4.0")
    private Double minRating;
}
