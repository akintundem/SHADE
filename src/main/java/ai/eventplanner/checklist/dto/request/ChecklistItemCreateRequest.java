package ai.eventplanner.checklist.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChecklistItemCreateRequest {
    private UUID eventId;
    private String title;
    private String description;
    private Boolean isCompleted;
    private LocalDateTime dueDate;
    private String priority;
    private UUID assignedTo;
    private String category;
}
