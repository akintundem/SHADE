package eventplanner.features.event.controller;

import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.dto.request.EventReminderRequest;
import eventplanner.features.event.dto.response.EventNotificationResponse;
import eventplanner.features.event.dto.response.EventNotificationSettingsResponse;
import eventplanner.features.event.dto.response.EventReminderResponse;
import eventplanner.features.event.service.EventNotificationService;
import eventplanner.features.event.service.EventNotificationSettingsService;
import eventplanner.features.event.service.EventReminderService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.service.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;

/**
 * Event Notifications and Reminders Controller
 * Handles event notifications, reminders, and communication
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Notifications", description = "Event notifications and reminders operations")
@SecurityRequirement(name = "bearerAuth")
public class EventNotificationController {

    private final EventNotificationService notificationService;
    private final EventNotificationSettingsService settingsService;
    private final EventReminderService reminderService;
    private final AuthorizationService authorizationService;

    public EventNotificationController(EventNotificationService notificationService,
                                       EventNotificationSettingsService settingsService,
                                       EventReminderService reminderService,
                                       AuthorizationService authorizationService) {
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.reminderService = reminderService;
        this.authorizationService = authorizationService;
    }

    // ==================== EVENT NOTIFICATION ENDPOINTS ====================

    @GetMapping("/{id}/notifications")
    @RequiresPermission(value = RbacPermissions.EVENT_NOTIFICATION_READ, resources = {"event_id=#id"})
    @Operation(summary = "Get event notification settings", description = "Get notification settings for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification settings retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<EventNotificationSettingsResponse> getNotificationSettings(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Verify user has access to the event
        if (!authorizationService.canAccessEvent(principal, id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event notification settings");
        }
        return ResponseEntity.ok(settingsService.getSettings(id));
    }

    @PostMapping("/{id}/notifications/send")
    @RequiresPermission(value = RbacPermissions.EVENT_NOTIFICATION_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Send event notification", description = "Send a notification for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification sent successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<EventNotificationResponse> sendNotification(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventNotificationRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Verify user is owner or admin
        if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can send notifications");
        }
        return ResponseEntity.ok(notificationService.sendNotification(id, request));
    }

    // ==================== EVENT REMINDER ENDPOINTS ====================

    @GetMapping("/{id}/reminders")
    @RequiresPermission(value = RbacPermissions.EVENT_REMINDER_READ, resources = {"event_id=#id"})
    @Operation(summary = "Get event reminders", description = "Get all reminders for an event")
    public ResponseEntity<List<EventReminderResponse>> getReminders(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Verify user has access to the event
        if (!authorizationService.canAccessEvent(principal, id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event reminders");
        }
        return ResponseEntity.ok(reminderService.list(id, page, size));
    }

    @PostMapping("/{id}/reminders")
    @RequiresPermission(value = RbacPermissions.EVENT_REMINDER_CREATE, resources = {"event_id=#id"})
    @Operation(summary = "Create event reminder", description = "Create a new reminder for an event")
    public ResponseEntity<EventReminderResponse> createReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventReminderRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Verify user is owner or admin
        if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can create reminders");
        }
        return ResponseEntity.ok(reminderService.create(id, request));
    }

    @PutMapping("/{id}/reminders/{reminderId}")
    @RequiresPermission(value = RbacPermissions.EVENT_REMINDER_UPDATE, resources = {"event_id=#id"})
    @Operation(summary = "Update event reminder", description = "Update an existing reminder")
    public ResponseEntity<EventReminderResponse> updateReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EventReminderRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Verify user is owner or admin
        if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can update reminders");
        }
        return ResponseEntity.ok(reminderService.update(id, reminderId, request));
    }

    @DeleteMapping("/{id}/reminders/{reminderId}")
    @RequiresPermission(value = RbacPermissions.EVENT_REMINDER_DELETE, resources = {"event_id=#id"})
    @Operation(summary = "Delete event reminder", description = "Delete a reminder")
    public ResponseEntity<Void> deleteReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Verify user is owner or admin
        if (!authorizationService.isEventOwner(principal, id) && !authorizationService.isAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only event owners or admins can delete reminders");
        }
        reminderService.delete(id, reminderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/reminders/{reminderId}")
    @RequiresPermission(value = RbacPermissions.EVENT_REMINDER_READ, resources = {"event_id=#id"})
    @Operation(summary = "Get specific reminder", description = "Get details of a specific reminder")
    public ResponseEntity<EventReminderResponse> getReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        // Verify user has access to the event
        if (!authorizationService.canAccessEvent(principal, id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to event reminder");
        }
        return ResponseEntity.ok(reminderService.get(id, reminderId));
    }
}
