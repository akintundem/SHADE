package eventplanner.features.event.controller;

import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.dto.request.EventNotificationSettingsRequest;
import eventplanner.features.event.dto.request.EventReminderRequest;
import eventplanner.features.event.dto.response.EventNotificationResponse;
import eventplanner.features.event.dto.response.EventNotificationSettingsResponse;
import eventplanner.features.event.dto.response.EventReminderResponse;
import eventplanner.features.event.service.EventNotificationService;
import eventplanner.features.event.service.EventNotificationSettingsService;
import eventplanner.features.event.service.EventReminderService;
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

    public EventNotificationController(EventNotificationService notificationService,
                                       EventNotificationSettingsService settingsService,
                                       EventReminderService reminderService) {
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.reminderService = reminderService;
    }

    // ==================== EVENT NOTIFICATION ENDPOINTS ====================

    @GetMapping("/{id}/notifications")
    @Operation(summary = "Get event notification settings", description = "Get notification settings for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification settings retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<EventNotificationSettingsResponse> getNotificationSettings(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        return ResponseEntity.ok(settingsService.getSettings(id));
    }

    @PutMapping("/{id}/notifications")
    @Operation(summary = "Update event notification settings", description = "Update notification settings for an event")
    public ResponseEntity<EventNotificationSettingsResponse> updateNotificationSettings(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventNotificationSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(id, request));
    }

    @PostMapping("/{id}/notifications/send")
    @Operation(summary = "Send event notification", description = "Send a notification for an event")
    public ResponseEntity<EventNotificationResponse> sendNotification(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventNotificationRequest request) {
        return ResponseEntity.ok(notificationService.sendNotification(id, request));
    }

    // ==================== EVENT REMINDER ENDPOINTS ====================

    @GetMapping("/{id}/reminders")
    @Operation(summary = "Get event reminders", description = "Get all reminders for an event")
    public ResponseEntity<List<EventReminderResponse>> getReminders(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(reminderService.list(id, page, size));
    }

    @PostMapping("/{id}/reminders")
    @Operation(summary = "Create event reminder", description = "Create a new reminder for an event")
    public ResponseEntity<EventReminderResponse> createReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventReminderRequest request) {
        return ResponseEntity.ok(reminderService.create(id, request));
    }

    @PutMapping("/{id}/reminders/{reminderId}")
    @Operation(summary = "Update event reminder", description = "Update an existing reminder")
    public ResponseEntity<EventReminderResponse> updateReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId,
            @Valid @RequestBody EventReminderRequest request) {
        return ResponseEntity.ok(reminderService.update(id, reminderId, request));
    }

    @DeleteMapping("/{id}/reminders/{reminderId}")
    @Operation(summary = "Delete event reminder", description = "Delete a reminder")
    public ResponseEntity<Void> deleteReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId) {
        reminderService.delete(id, reminderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/reminders/{reminderId}")
    @Operation(summary = "Get specific reminder", description = "Get details of a specific reminder")
    public ResponseEntity<EventReminderResponse> getReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId) {
        return ResponseEntity.ok(reminderService.get(id, reminderId));
    }
}
