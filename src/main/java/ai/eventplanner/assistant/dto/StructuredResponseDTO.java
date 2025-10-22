package ai.eventplanner.assistant.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StructuredResponseDTO {
    private String responseType;  // "text", "venue_cards", "email_template", "chips", "mixed"
    private String text;
    private List<VenueCardDTO> venueCards;
    private EmailTemplateDTO emailTemplate;
    private List<ChipDTO> chips;
    private List<ActionButtonDTO> actionButtons;
    private Map<String, Object> metadata;
}


