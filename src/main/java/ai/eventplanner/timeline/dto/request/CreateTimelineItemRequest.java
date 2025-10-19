package ai.eventplanner.timeline.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateTimelineItemRequest {
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private LocalDateTime scheduledAt;
    
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    
    private UUID assignedTo;
    
    private UUID[] dependencies;
}
