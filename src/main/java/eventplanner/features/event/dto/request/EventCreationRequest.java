package eventplanner.features.event.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventCreationRequest {
    
    @NotBlank(message = "Event name is required")
    @Size(min = 1, max = 255, message = "Event name must be between 1 and 255 characters")
    private String name;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @NotBlank(message = "Owner ID is required")
    private String ownerId;
    
    private String eventType;
    
    @NotNull(message = "Start date time is required")
    @Future(message = "Start date time must be in the future")
    private LocalDateTime startDateTime;
    
    private LocalDateTime endDateTime;
    
    @Min(value = 0, message = "Capacity cannot be negative")
    @Max(value = 100000, message = "Capacity cannot exceed 100,000")
    private Integer capacity;
    
    private Boolean isPublic = true;
    private Boolean requiresApproval = false;
    private Boolean qrCodeEnabled = false;
    
    private List<VenueRequest> venues;
    
    @Data
    public static class VenueRequest {
        @NotBlank(message = "Venue name is required")
        private String name;
        
        @NotBlank(message = "Venue address is required")
        private String address;
        
        private String description;
        private String contactEmail;
        private String contactPhone;
        private String website;
    }
}
