package ai.eventplanner.event.controller;

import ai.eventplanner.event.dto.request.EventNotificationRequest;
import ai.eventplanner.event.dto.request.EventReminderRequest;
import ai.eventplanner.event.dto.response.EventNotificationResponse;
import ai.eventplanner.event.dto.response.EventNotificationSettingsResponse;
import ai.eventplanner.event.dto.response.EventReminderResponse;
import ai.eventplanner.comms.entity.Communication;
import ai.eventplanner.comms.service.NotificationService;
import ai.eventplanner.event.service.EventReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Event Notifications and Reminders Controller
 * Handles event notifications, reminders, and communication
 */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Event Notifications", description = "Event notifications and reminders operations")
@SecurityRequirement(name = "bearerAuth")
public class EventNotificationController {

    private final NotificationService notificationService;
    private final EventReminderService reminderService;

    public EventNotificationController(NotificationService notificationService, EventReminderService reminderService) {
        this.notificationService = notificationService;
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
        EventNotificationSettingsResponse response = new EventNotificationSettingsResponse();
        response.setEventId(id);
        response.setEmailNotifications(true);
        response.setSmsNotifications(false);
        response.setPushNotifications(true);
        response.setReminderEnabled(true);
        response.setDefaultReminderTime("24h");
        response.setAvailableChannels(Arrays.asList("email", "sms", "push"));
        response.setAvailableTemplates(Arrays.asList("event_reminder", "event_update", "event_cancelled"));
        response.setUpdatedAt(LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/notifications")
    @Operation(summary = "Update event notification settings", description = "Update notification settings for an event")
    public ResponseEntity<Map<String, Object>> updateNotificationSettings(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestBody Map<String, Object> settings) {
        Map<String, Object> response = new HashMap<>(settings);
        response.put("eventId", id);
        response.put("updatedAt", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/notifications/send")
    @Operation(summary = "Send event notification", description = "Send a notification for an event")
    public ResponseEntity<EventNotificationResponse> sendNotification(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventNotificationRequest request) {
        Communication c = notificationService.send(
                id,
                request.getChannel(),
                request.getSubject(),
                request.getContent(),
                request.getRecipientEmails(),
                request.getScheduledAt(),
                request.getPriority()
        );

        EventNotificationResponse response = new EventNotificationResponse();
        response.setNotificationId(c.getId());
        response.setEventId(id);
        response.setChannel(request.getChannel());
        response.setSubject(request.getSubject());
        response.setContent(request.getContent());
        response.setStatus(c.getStatus().name().toLowerCase());
        response.setRecipientCount(
                (request.getRecipientUserIds() != null ? request.getRecipientUserIds().size() : 0) +
                (request.getRecipientEmails() != null ? request.getRecipientEmails().size() : 0)
        );
        response.setScheduledAt(request.getScheduledAt());
        response.setSentAt(c.getSentAt());
        response.setPriority(request.getPriority());
        response.setCreatedAt(c.getCreatedAt());
        response.setSuccessfulRecipients(request.getRecipientEmails() != null ? request.getRecipientEmails() : new ArrayList<>());
        response.setFailedRecipients(new ArrayList<>());

        return ResponseEntity.ok(response);
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
