package ai.eventplanner.assistant.dto;

import lombok.Data;

@Data
public class EmailTemplateDTO {
    private String id;
    private String toName;
    private String toEmail;
    private String subject;
    private String message;
    private String templateType;
    private String venueId;
}


