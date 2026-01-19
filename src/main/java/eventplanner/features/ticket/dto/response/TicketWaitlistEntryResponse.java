package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.entity.TicketWaitlistEntry;
import eventplanner.features.ticket.enums.TicketWaitlistStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketWaitlistEntryResponse {

    private UUID id;
    private UUID eventId;
    private UUID ticketTypeId;
    private String ticketTypeName;
    private UUID requesterUserId;
    private String requesterEmail;
    private String requesterName;
    private TicketWaitlistStatus status;
    private Integer quantity;
    private UUID fulfilledBy;
    private LocalDateTime fulfilledAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketWaitlistEntryResponse from(TicketWaitlistEntry entry) {
        TicketWaitlistEntryResponse response = new TicketWaitlistEntryResponse();
        response.setId(entry.getId());
        response.setEventId(entry.getEvent() != null ? entry.getEvent().getId() : null);
        response.setTicketTypeId(entry.getTicketType() != null ? entry.getTicketType().getId() : null);
        response.setTicketTypeName(entry.getTicketType() != null ? entry.getTicketType().getName() : null);
        response.setRequesterUserId(entry.getRequester() != null ? entry.getRequester().getId() : null);
        response.setRequesterEmail(entry.getRequesterEmail());
        response.setRequesterName(entry.getRequesterName());
        response.setStatus(entry.getStatus());
        response.setQuantity(entry.getQuantity());
        response.setFulfilledBy(entry.getFulfilledBy() != null ? entry.getFulfilledBy().getId() : null);
        response.setFulfilledAt(entry.getFulfilledAt());
        response.setCancelledAt(entry.getCancelledAt());
        response.setCreatedAt(entry.getCreatedAt());
        response.setUpdatedAt(entry.getUpdatedAt());
        return response;
    }
}
