package ai.eventplanner.event.controller;

import ai.eventplanner.event.dto.request.EventNotificationRequest;
import ai.eventplanner.event.dto.request.EventReminderRequest;
import ai.eventplanner.event.dto.response.EventNotificationResponse;
import ai.eventplanner.event.dto.response.EventNotificationSettingsResponse;
import ai.eventplanner.event.dto.response.EventReminderResponse;
import ai.eventplanner.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    private final EventService eventService;

    public EventNotificationController(EventService eventService) {
        this.eventService = eventService;
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
        try {
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
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/notifications")
    @Operation(summary = "Update event notification settings", description = "Update notification settings for an event")
    public ResponseEntity<Map<String, Object>> updateNotificationSettings(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @RequestBody Map<String, Object> settings) {
        try {
            Map<String, Object> response = new HashMap<>(settings);
            response.put("eventId", id);
            response.put("updatedAt", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/notifications/send")
    @Operation(summary = "Send event notification", description = "Send a notification for an event")
    public ResponseEntity<EventNotificationResponse> sendNotification(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventNotificationRequest request) {
        try {
            EventNotificationResponse response = new EventNotificationResponse();
            response.setNotificationId(UUID.randomUUID());
            response.setEventId(id);
            response.setChannel(request.getChannel());
            response.setSubject(request.getSubject());
            response.setContent(request.getContent());
            response.setStatus("sent");
            response.setRecipientCount(
                    (request.getRecipientUserIds() != null ? request.getRecipientUserIds().size() : 0) +
                    (request.getRecipientEmails() != null ? request.getRecipientEmails().size() : 0)
            );
            response.setScheduledAt(request.getScheduledAt());
            response.setSentAt(request.getScheduledAt() != null ? null : LocalDateTime.now());
            response.setPriority(request.getPriority());
            response.setCreatedAt(LocalDateTime.now());
            
            // Simulate successful and failed recipients
            List<String> allRecipients = new ArrayList<>();
            if (request.getRecipientEmails() != null) {
                allRecipients.addAll(request.getRecipientEmails());
            }
            response.setSuccessfulRecipients(allRecipients);
            response.setFailedRecipients(new ArrayList<>());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    // ==================== EVENT REMINDER ENDPOINTS ====================

    @GetMapping("/{id}/reminders")
    @Operation(summary = "Get event reminders", description = "Get all reminders for an event")
    public ResponseEntity<List<EventReminderResponse>> getReminders(
            @Parameter(description = "Event ID") @PathVariable UUID id) {
        try {
            // For now, return empty list - this would be populated from a reminders table
            List<EventReminderResponse> reminders = new ArrayList<>();
            return ResponseEntity.ok(reminders);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/reminders")
    @Operation(summary = "Create event reminder", description = "Create a new reminder for an event")
    public ResponseEntity<EventReminderResponse> createReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Valid @RequestBody EventReminderRequest request) {
        try {
            EventReminderResponse response = new EventReminderResponse();
            response.setReminderId(UUID.randomUUID());
            response.setEventId(id);
            response.setTitle(request.getTitle());
            response.setDescription(request.getDescription());
            response.setReminderTime(request.getReminderTime());
            response.setChannel(request.getChannel());
            response.setReminderType(request.getReminderType());
            response.setIsActive(request.getIsActive());
            response.setCustomMessage(request.getCustomMessage());
            response.setRecipientCount(
                    (request.getRecipientUserIds() != null ? request.getRecipientUserIds().size() : 0) +
                    (request.getRecipientEmails() != null ? request.getRecipientEmails().size() : 0)
            );
            response.setCreatedAt(LocalDateTime.now());
            response.setUpdatedAt(LocalDateTime.now());
            response.setWasSent(false);
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/{id}/reminders/{reminderId}")
    @Operation(summary = "Update event reminder", description = "Update an existing reminder")
    public ResponseEntity<EventReminderResponse> updateReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId,
            @Valid @RequestBody EventReminderRequest request) {
        try {
            EventReminderResponse response = new EventReminderResponse();
            response.setReminderId(reminderId);
            response.setEventId(id);
            response.setTitle(request.getTitle());
            response.setDescription(request.getDescription());
            response.setReminderTime(request.getReminderTime());
            response.setChannel(request.getChannel());
            response.setReminderType(request.getReminderType());
            response.setIsActive(request.getIsActive());
            response.setCustomMessage(request.getCustomMessage());
            response.setRecipientCount(
                    (request.getRecipientUserIds() != null ? request.getRecipientUserIds().size() : 0) +
                    (request.getRecipientEmails() != null ? request.getRecipientEmails().size() : 0)
            );
            response.setUpdatedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{id}/reminders/{reminderId}")
    @Operation(summary = "Delete event reminder", description = "Delete a reminder")
    public ResponseEntity<Void> deleteReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId) {
        try {
            // In a real implementation, this would delete the reminder from the database
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/reminders/{reminderId}")
    @Operation(summary = "Get specific reminder", description = "Get details of a specific reminder")
    public ResponseEntity<EventReminderResponse> getReminder(
            @Parameter(description = "Event ID") @PathVariable UUID id,
            @Parameter(description = "Reminder ID") @PathVariable UUID reminderId) {
        try {
            EventReminderResponse response = new EventReminderResponse();
            response.setReminderId(reminderId);
            response.setEventId(id);
            response.setTitle("Sample Reminder");
            response.setDescription("This is a sample reminder");
            response.setReminderTime(LocalDateTime.now().plusHours(24));
            response.setChannel("email");
            response.setReminderType("custom");
            response.setIsActive(true);
            response.setCreatedAt(LocalDateTime.now());
            response.setUpdatedAt(LocalDateTime.now());
            response.setWasSent(false);
            
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
