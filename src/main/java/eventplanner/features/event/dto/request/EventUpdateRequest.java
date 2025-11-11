package eventplanner.features.event.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventUpdateRequest {
    
    @Size(min = 1, max = 255, message = "Event name must be between 1 and 255 characters")
    private String name;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    private String eventType;
    
    @Future(message = "Start date time must be in the future")
    private LocalDateTime startDateTime;
    
    private LocalDateTime endDateTime;
    
    @Min(value = 0, message = "Capacity cannot be negative")
    @Max(value = 100000, message = "Capacity cannot exceed 100,000")
    private Integer capacity;
    
    private Boolean isPublic;
    private Boolean requiresApproval;
    private Boolean qrCodeEnabled;
    
    private eventplanner.features.event.dto.VenueDTO venue;
}
