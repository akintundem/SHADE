package eventplanner.features.attendee.dto.response;

import eventplanner.common.domain.enums.EventUserType;
import eventplanner.common.domain.enums.RegistrationStatus;
import eventplanner.common.domain.enums.RoleName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventUserResponse {
    
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private EventUserType userType;
    private RegistrationStatus registrationStatus;
    private LocalDateTime registrationDate;
    private List<EventRoleResponse> roles;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventRoleResponse {
        private UUID id;
        private RoleName roleName;
        private String permissions;
        private Boolean isActive;
        private LocalDateTime assignedDate;
        private String notes;
    }
}
