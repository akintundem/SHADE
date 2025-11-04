package eventplanner.features.attendee.dto.request;

import eventplanner.common.domain.enums.RoleName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Role name is required")
    private RoleName roleName;
    
    private String permissions;
    
    private String notes;
}
