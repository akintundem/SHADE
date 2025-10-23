package ai.eventplanner.attendee.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    
    private UUID eventId;
    private List<InvitationDetail> invitations;
    private Long totalSent;
    private Long totalDelivered;
    private Long totalFailed;
    private LocalDateTime sentAt;
    private String status;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvitationDetail {
        private String email;
        private String status;
        private String message;
        private LocalDateTime sentAt;
        private LocalDateTime deliveredAt;
        private String errorMessage;
    }
}
