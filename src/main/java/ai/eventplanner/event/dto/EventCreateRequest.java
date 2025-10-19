package ai.eventplanner.event.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class EventCreateRequest {

    private UUID organizationId;

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    private LocalDateTime date;

    private UUID venueId;

    private Map<String, Object> metadata;

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public UUID getVenueId() { return venueId; }
    public void setVenueId(UUID venueId) { this.venueId = venueId; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
