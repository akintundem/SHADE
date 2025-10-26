package ai.eventplanner.checklist.dto.request;

import lombok.Data;

@Data
public class ChecklistTemplateRequest {
    private String templateName;
    private String eventType;
}
