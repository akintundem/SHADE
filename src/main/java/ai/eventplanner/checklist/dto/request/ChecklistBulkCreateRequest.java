package ai.eventplanner.checklist.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ChecklistBulkCreateRequest {
    private List<ChecklistItemCreateRequest> items;
}
