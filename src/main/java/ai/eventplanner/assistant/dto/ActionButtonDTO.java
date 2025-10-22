package ai.eventplanner.assistant.dto;

import lombok.Data;

@Data
public class ActionButtonDTO {
    private String id;
    private String label;
    private String icon;
    private String action;
    private String style;
    private boolean disabled;
}


