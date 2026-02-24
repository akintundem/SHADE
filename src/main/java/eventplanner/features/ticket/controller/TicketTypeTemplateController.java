package eventplanner.features.ticket.controller;

import eventplanner.features.ticket.dto.request.ApplyTicketTypeTemplateRequest;
import eventplanner.features.ticket.dto.request.CreateTicketTypeTemplateRequest;
import eventplanner.features.ticket.dto.request.UpdateTicketTypeTemplateRequest;
import eventplanner.features.ticket.dto.response.TicketTypeResponse;
import eventplanner.features.ticket.dto.response.TicketTypeTemplateResponse;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.entity.TicketTypeTemplate;
import eventplanner.features.ticket.service.TicketTypeTemplateService;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.auth.service.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ticket-type-templates")
@Tag(name = "Ticket Type Templates")
@RequiredArgsConstructor
public class TicketTypeTemplateController {

    private final TicketTypeTemplateService templateService;

    @PostMapping
    @Operation(summary = "Create ticket type template", description = "Create a reusable ticket type template.")
    @RequiresPermission(RbacPermissions.TICKET_TYPE_TEMPLATE_CREATE)
    public ResponseEntity<TicketTypeTemplateResponse> createTemplate(
            @Valid @RequestBody CreateTicketTypeTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketTypeTemplate template = templateService.createTemplate(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketTypeTemplateResponse.from(template));
    }

    @GetMapping
    @Operation(summary = "List ticket type templates", description = "List ticket type templates for the authenticated user.")
    @RequiresPermission(RbacPermissions.TICKET_TYPE_TEMPLATE_READ)
    public ResponseEntity<List<TicketTypeTemplateResponse>> listTemplates(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TicketTypeTemplateResponse> responses = templateService.listTemplates(principal).stream()
            .map(TicketTypeTemplateResponse::from)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "Update ticket type template", description = "Update a ticket type template.")
    @RequiresPermission(RbacPermissions.TICKET_TYPE_TEMPLATE_UPDATE)
    public ResponseEntity<TicketTypeTemplateResponse> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTicketTypeTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketTypeTemplate template = templateService.updateTemplate(templateId, request, principal);
        return ResponseEntity.ok(TicketTypeTemplateResponse.from(template));
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "Delete ticket type template", description = "Delete a ticket type template.")
    @RequiresPermission(RbacPermissions.TICKET_TYPE_TEMPLATE_DELETE)
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal UserPrincipal principal) {
        templateService.deleteTemplate(templateId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/apply/events/{eventId}")
    @Operation(summary = "Apply template to event", description = "Create a ticket type from a template for an event.")
    @RequiresPermission(RbacPermissions.TICKET_TYPE_CREATE)
    public ResponseEntity<TicketTypeResponse> applyTemplate(
            @PathVariable UUID templateId,
            @PathVariable UUID eventId,
            @RequestBody(required = false) ApplyTicketTypeTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketType ticketType = templateService.applyTemplate(templateId, eventId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketTypeResponse.from(ticketType));
    }
}
