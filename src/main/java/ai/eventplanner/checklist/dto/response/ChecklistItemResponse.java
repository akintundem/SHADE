package ai.eventplanner.checklist.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ChecklistItemResponse {
    private UUID id;
    private UUID eventId;
    private String title;
    private String description;
    private Boolean isCompleted;
    private LocalDateTime dueDate;
    private String priority;
    private UUID assignedTo;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
