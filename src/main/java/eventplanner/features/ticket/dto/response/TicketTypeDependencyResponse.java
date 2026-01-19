package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.entity.TicketTypeDependency;
import lombok.Data;

import java.util.UUID;

@Data
public class TicketTypeDependencyResponse {

    private UUID id;
    private UUID requiredTicketTypeId;
    private String requiredTicketTypeName;
    private Integer minQuantity;

    public static TicketTypeDependencyResponse from(TicketTypeDependency dependency) {
        TicketTypeDependencyResponse response = new TicketTypeDependencyResponse();
        response.setId(dependency.getId());
        response.setRequiredTicketTypeId(dependency.getRequiredTicketType() != null
            ? dependency.getRequiredTicketType().getId() : null);
        response.setRequiredTicketTypeName(dependency.getRequiredTicketType() != null
            ? dependency.getRequiredTicketType().getName() : null);
        response.setMinQuantity(dependency.getMinQuantity());
        return response;
    }
}
