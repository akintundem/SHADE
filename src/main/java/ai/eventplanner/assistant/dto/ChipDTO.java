package ai.eventplanner.assistant.dto;

import lombok.Data;

@Data
public class ChipDTO {
    private String id;
    private String label;
    private String icon;
    private boolean selected;
    private String action;
}


