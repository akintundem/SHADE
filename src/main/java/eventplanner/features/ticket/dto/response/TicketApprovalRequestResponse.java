package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.entity.TicketApprovalRequest;
import eventplanner.features.ticket.enums.TicketApprovalStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TicketApprovalRequestResponse {

    private UUID id;
    private UUID eventId;
    private UUID ticketTypeId;
    private String ticketTypeName;
    private UUID requesterUserId;
    private String requesterEmail;
    private String requesterName;
    private TicketApprovalStatus status;
    private Integer quantity;
    private UUID decidedBy;
    private LocalDateTime decidedAt;
    private String decisionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketApprovalRequestResponse from(TicketApprovalRequest request) {
        TicketApprovalRequestResponse response = new TicketApprovalRequestResponse();
        response.setId(request.getId());
        response.setEventId(request.getEvent() != null ? request.getEvent().getId() : null);
        response.setTicketTypeId(request.getTicketType() != null ? request.getTicketType().getId() : null);
        response.setTicketTypeName(request.getTicketType() != null ? request.getTicketType().getName() : null);
        response.setRequesterUserId(request.getRequester() != null ? request.getRequester().getId() : null);
        response.setRequesterEmail(request.getRequesterEmail());
        response.setRequesterName(request.getRequesterName());
        response.setStatus(request.getStatus());
        response.setQuantity(request.getQuantity());
        response.setDecidedBy(request.getDecidedBy() != null ? request.getDecidedBy().getId() : null);
        response.setDecidedAt(request.getDecidedAt());
        response.setDecisionNote(request.getDecisionNote());
        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());
        return response;
    }
}
