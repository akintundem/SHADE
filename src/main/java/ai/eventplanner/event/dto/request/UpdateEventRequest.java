package ai.eventplanner.event.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UpdateEventRequest {
    
    @Size(max = 255, message = "Event name must not exceed 255 characters")
    private String name;
    
    @Size(max = 50, message = "Event type must not exceed 50 characters")
    private String type;
    
    private LocalDateTime date;
    
    private UUID venueId;
    
    private String status;
    
    private String metadata;
}
