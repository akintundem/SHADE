package eventplanner.features.attendee.controller;

import eventplanner.features.attendee.dto.request.*;
import eventplanner.features.attendee.dto.response.*;
import eventplanner.features.attendee.service.AttendeeManagementService;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.common.domain.enums.EventUserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events/{eventId}")
@Tag(name = "Attendee Management", description = "Comprehensive attendee management endpoints")
public class AttendeeManagementController {
    
    private final AttendeeManagementService attendeeManagementService;
    
    public AttendeeManagementController(AttendeeManagementService attendeeManagementService) {
        this.attendeeManagementService = attendeeManagementService;
    }
    
    // Core Attendance Management
    @PostMapping("/attendances")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Register for event", description = "Register a user for an event with full attendance details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully registered for event"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "User already registered for event")
    })
    public ResponseEntity<AttendanceDetailResponse> registerForEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateAttendanceRequest request) {
        try {
            request.setEventId(eventId);
            AttendanceDetailResponse response = attendeeManagementService.registerForEvent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @PostMapping("/attendances/bulk")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CREATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Bulk register attendees", description = "Register multiple attendees for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully registered attendees"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> bulkRegister(
            @PathVariable UUID eventId,
            @Valid @RequestBody BulkAttendanceRequest request) {
        try {
            request.setEventId(eventId);
            List<AttendanceDetailResponse> responses = attendeeManagementService.bulkRegister(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(responses);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get all attendees for event", description = "Retrieve all attendees registered for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved attendees"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> getAllAttendees(@PathVariable UUID eventId) {
        try {
            List<AttendanceDetailResponse> responses = attendeeManagementService.getAllAttendances(eventId);
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/{attendanceId}")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Get specific attendance", description = "Retrieve details for a specific attendance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance"),
        @ApiResponse(responseCode = "404", description = "Attendance not found")
    })
    public ResponseEntity<AttendanceDetailResponse> getAttendance(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId) {
        try {
            AttendanceDetailResponse response = attendeeManagementService.getAttendanceById(attendanceId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @PutMapping("/attendances/{attendanceId}")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_UPDATE, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Update attendance details", description = "Update attendance information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated attendance"),
        @ApiResponse(responseCode = "404", description = "Attendance not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<AttendanceDetailResponse> updateAttendance(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        try {
            AttendanceDetailResponse response = attendeeManagementService.updateAttendance(attendanceId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @DeleteMapping("/attendances/{attendanceId}")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_DELETE, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Cancel attendance", description = "Cancel a user's attendance for an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully cancelled attendance"),
        @ApiResponse(responseCode = "404", description = "Attendance not found")
    })
    public ResponseEntity<Void> cancelAttendance(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId) {
        try {
            attendeeManagementService.cancelAttendance(attendanceId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    // Check-in/Check-out Management
    @PostMapping("/attendances/{attendanceId}/check-in")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CHECKIN, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Check in attendee", description = "Check in an attendee for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked in"),
        @ApiResponse(responseCode = "404", description = "Attendance not found"),
        @ApiResponse(responseCode = "400", description = "Invalid check-in data")
    })
    public ResponseEntity<CheckInResponse> checkInAttendee(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId,
            @Valid @RequestBody CheckInRequest request) {
        try {
            CheckInResponse response = attendeeManagementService.checkInAttendee(attendanceId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @PostMapping("/attendances/{attendanceId}/check-out")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_CHECKOUT, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Check out attendee", description = "Check out an attendee from the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully checked out"),
        @ApiResponse(responseCode = "404", description = "Attendance not found")
    })
    public ResponseEntity<CheckInResponse> checkOutAttendee(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId) {
        try {
            CheckInResponse response = attendeeManagementService.checkOutAttendee(attendanceId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/checked-in")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get checked-in attendees", description = "Retrieve all attendees who have checked in")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved checked-in attendees")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> getCheckedInAttendees(@PathVariable UUID eventId) {
        try {
            List<AttendanceDetailResponse> responses = attendeeManagementService.getCheckedInAttendees(eventId);
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/attendance-stats")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_ANALYTICS_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get attendance statistics", description = "Get comprehensive attendance statistics for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance statistics")
    })
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceStats(@PathVariable UUID eventId) {
        try {
            AttendanceSummaryResponse response = attendeeManagementService.getAttendanceSummary(eventId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    // QR Code Management
    @GetMapping("/attendances/{attendanceId}/qr-code")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_QR_READ, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Get attendee QR code", description = "Retrieve QR code for a specific attendee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved QR code"),
        @ApiResponse(responseCode = "404", description = "Attendance not found")
    })
    public ResponseEntity<String> getAttendeeQRCode(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId) {
        try {
            String qrCode = attendeeManagementService.getAttendeeQRCode(attendanceId);
            return ResponseEntity.ok(qrCode);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @PostMapping("/attendances/scan-qr")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_QR_SCAN, resources = {"event_id=#eventId"})
    @Operation(summary = "Scan QR code for check-in", description = "Scan and validate QR code for attendee check-in")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully scanned QR code"),
        @ApiResponse(responseCode = "400", description = "Invalid QR code")
    })
    public ResponseEntity<CheckInResponse> scanQRCode(
            @PathVariable UUID eventId,
            @RequestParam String qrCode) {
        try {
            CheckInResponse response = attendeeManagementService.scanQRCode(eventId, qrCode);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @PostMapping("/attendances/{attendanceId}/regenerate-qr")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_QR_REGENERATE, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Regenerate QR code", description = "Generate a new QR code for an attendee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully regenerated QR code"),
        @ApiResponse(responseCode = "404", description = "Attendance not found")
    })
    public ResponseEntity<String> regenerateQRCode(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId) {
        try {
            String newQrCode = attendeeManagementService.regenerateQRCode(attendanceId);
            return ResponseEntity.ok(newQrCode);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    // User-Event Relationship Management
    @PostMapping("/users")
    @RequiresPermission(value = RbacPermissions.ROLE_ASSIGN, resources = {"event_id=#eventId"})
    @Operation(summary = "Add user to event", description = "Add a user to an event with specific user type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully added user to event"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<EventUserResponse> addUserToEvent(
            @PathVariable UUID eventId,
            @RequestParam UUID userId,
            @RequestParam String userType) {
        try {
            EventUserType userTypeEnum = EventUserType.valueOf(userType.toUpperCase());
            EventUserResponse response = attendeeManagementService.addUserToEvent(eventId, userId, userTypeEnum);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user type: " + userType, e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @GetMapping("/users")
    @RequiresPermission(value = RbacPermissions.ROLE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get all event users", description = "Retrieve all users associated with an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved event users")
    })
    public ResponseEntity<List<EventUserResponse>> getAllEventUsers(@PathVariable UUID eventId) {
        try {
            List<EventUserResponse> responses = attendeeManagementService.getAllEventUsers(eventId);
            return ResponseEntity.ok(responses);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/users/{userId}")
    @RequiresPermission(value = RbacPermissions.ROLE_READ, resources = {"user_id=#userId", "event_id=#eventId"})
    @Operation(summary = "Get specific event user", description = "Retrieve details for a specific event user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved event user"),
        @ApiResponse(responseCode = "404", description = "Event user not found")
    })
    public ResponseEntity<EventUserResponse> getEventUser(
            @PathVariable UUID eventId,
            @PathVariable UUID userId) {
        try {
            EventUserResponse response = attendeeManagementService.getEventUser(eventId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @PutMapping("/users/{userId}")
    @RequiresPermission(value = RbacPermissions.ROLE_UPDATE, resources = {"user_id=#userId", "event_id=#eventId"})
    @Operation(summary = "Update event user details", description = "Update event user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated event user"),
        @ApiResponse(responseCode = "404", description = "Event user not found")
    })
    public ResponseEntity<EventUserResponse> updateEventUser(
            @PathVariable UUID eventId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            EventUserResponse response = attendeeManagementService.updateEventUser(eventId, userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @DeleteMapping("/users/{userId}")
    @RequiresPermission(value = RbacPermissions.ROLE_REMOVE, resources = {"user_id=#userId", "event_id=#eventId"})
    @Operation(summary = "Remove user from event", description = "Remove a user from an event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully removed user from event"),
        @ApiResponse(responseCode = "404", description = "Event user not found")
    })
    public ResponseEntity<Void> removeUserFromEvent(
            @PathVariable UUID eventId,
            @PathVariable UUID userId) {
        try {
            attendeeManagementService.removeUserFromEvent(eventId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    // Role Management
    @PostMapping("/users/{userId}/roles")
    @RequiresPermission(value = RbacPermissions.ROLE_ASSIGN, resources = {"user_id=#userId"})
    @Operation(summary = "Assign role to user", description = "Assign a specific role to a user for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully assigned role"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<EventUserResponse> assignRole(
            @PathVariable UUID eventId,
            @PathVariable UUID userId,
            @Valid @RequestBody AssignRoleRequest request) {
        try {
            EventUserResponse response = attendeeManagementService.assignRole(eventId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @GetMapping("/users/{userId}/roles")
    @RequiresPermission(value = RbacPermissions.ROLE_READ, resources = {"user_id=#userId", "event_id=#eventId"})
    @Operation(summary = "Get user roles", description = "Retrieve all roles assigned to a user for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user roles")
    })
    public ResponseEntity<List<EventUserResponse.EventRoleResponse>> getUserRoles(
            @PathVariable UUID eventId,
            @PathVariable UUID userId) {
        try {
            List<EventUserResponse.EventRoleResponse> roles = attendeeManagementService.getUserRoles(eventId, userId);
            return ResponseEntity.ok(roles);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @PutMapping("/users/{userId}/roles/{roleId}")
    @RequiresPermission(value = RbacPermissions.ROLE_UPDATE, resources = {"user_id=#userId", "event_id=#eventId"})
    @Operation(summary = "Update user role", description = "Update role details for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated role"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<EventUserResponse.EventRoleResponse> updateUserRole(
            @PathVariable UUID eventId,
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignRoleRequest request) {
        try {
            EventUserResponse.EventRoleResponse response = attendeeManagementService.updateUserRole(eventId, userId, roleId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @RequiresPermission(value = RbacPermissions.ROLE_REMOVE, resources = {"user_id=#userId", "event_id=#eventId"})
    @Operation(summary = "Remove user role", description = "Remove a role from a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully removed role"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<Void> removeUserRole(
            @PathVariable UUID eventId,
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        try {
            attendeeManagementService.removeUserRole(eventId, userId, roleId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    // Analytics and Reporting
    @GetMapping("/analytics/attendance")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_ANALYTICS_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get attendance analytics", description = "Get comprehensive attendance analytics and insights")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance analytics")
    })
    public ResponseEntity<AttendanceAnalyticsResponse> getAttendanceAnalytics(@PathVariable UUID eventId) {
        try {
            AttendanceAnalyticsResponse response = attendeeManagementService.getAttendanceAnalytics(eventId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/analytics/check-in-timeline")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_ANALYTICS_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get check-in timeline", description = "Get timeline of check-ins for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved check-in timeline")
    })
    public ResponseEntity<List<AttendanceAnalyticsResponse.CheckInTimeline>> getCheckInTimeline(@PathVariable UUID eventId) {
        try {
            List<AttendanceAnalyticsResponse.CheckInTimeline> timeline = attendeeManagementService.getCheckInTimeline(eventId);
            return ResponseEntity.ok(timeline);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/analytics/attendance-by-type")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_ANALYTICS_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get attendance by user type", description = "Get attendance breakdown by user type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance by type")
    })
    public ResponseEntity<Map<String, Long>> getAttendanceByType(@PathVariable UUID eventId) {
        try {
            Map<String, Long> attendanceByType = attendeeManagementService.getAttendanceByType(eventId);
            return ResponseEntity.ok(attendanceByType);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/analytics/no-shows")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_ANALYTICS_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get no-show analytics", description = "Get analytics for attendees who did not show up")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved no-show analytics")
    })
    public ResponseEntity<List<AttendanceAnalyticsResponse.NoShowAnalysis>> getNoShowAnalytics(@PathVariable UUID eventId) {
        try {
            List<AttendanceAnalyticsResponse.NoShowAnalysis> analysis = attendeeManagementService.getNoShowAnalytics(eventId);
            return ResponseEntity.ok(analysis);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/analytics/registration-timeline")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_ANALYTICS_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get registration timeline", description = "Get timeline of registrations for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved registration timeline")
    })
    public ResponseEntity<List<AttendanceAnalyticsResponse.RegistrationTimeline>> getRegistrationTimeline(@PathVariable UUID eventId) {
        try {
            List<AttendanceAnalyticsResponse.RegistrationTimeline> timeline = attendeeManagementService.getRegistrationTimeline(eventId);
            return ResponseEntity.ok(timeline);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    // Communication and Notifications
    @PostMapping("/attendances/bulk-email")
    @RequiresPermission(value = RbacPermissions.COMMUNICATION_SEND, resources = {"event_id=#eventId"})
    @Operation(summary = "Send bulk email to attendees", description = "Send email to multiple attendees")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully sent bulk email"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<Map<String, Object>> sendBulkEmail(
            @PathVariable UUID eventId,
            @Valid @RequestBody SendInvitationRequest request) {
        try {
            Map<String, Object> result = attendeeManagementService.sendBulkEmail(eventId, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @PostMapping("/attendances/{attendanceId}/notify")
    @RequiresPermission(value = RbacPermissions.COMMUNICATION_SEND, resources = {"attendance_id=#attendanceId", "event_id=#eventId"})
    @Operation(summary = "Send notification to attendee", description = "Send notification to a specific attendee")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully sent notification"),
        @ApiResponse(responseCode = "404", description = "Attendance not found")
    })
    public ResponseEntity<Map<String, Object>> sendNotification(
            @PathVariable UUID eventId,
            @PathVariable UUID attendanceId,
            @Valid @RequestBody SendInvitationRequest request) {
        try {
            Map<String, Object> result = attendeeManagementService.sendNotification(eventId, attendanceId, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/communication-history")
    @RequiresPermission(value = RbacPermissions.COMMUNICATION_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get communication history", description = "Get history of communications sent to attendees")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved communication history")
    })
    public ResponseEntity<List<Map<String, Object>>> getCommunicationHistory(@PathVariable UUID eventId) {
        try {
            List<Map<String, Object>> history = attendeeManagementService.getCommunicationHistory(eventId);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @PostMapping("/invitations/send")
    @RequiresPermission(value = RbacPermissions.COMMUNICATION_SEND, resources = {"event_id=#eventId"})
    @Operation(summary = "Send invitations", description = "Send invitations to potential attendees")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully sent invitations"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<InvitationResponse> sendInvitations(
            @PathVariable UUID eventId,
            @Valid @RequestBody SendInvitationRequest request) {
        try {
            InvitationResponse response = attendeeManagementService.sendInvitations(eventId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @GetMapping("/invitations")
    @RequiresPermission(value = RbacPermissions.COMMUNICATION_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get sent invitations", description = "Retrieve all invitations sent for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved invitations")
    })
    public ResponseEntity<List<InvitationResponse>> getSentInvitations(@PathVariable UUID eventId) {
        try {
            List<InvitationResponse> invitations = attendeeManagementService.getSentInvitations(eventId);
            return ResponseEntity.ok(invitations);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    // Export and Import
    @GetMapping("/attendances/export/csv")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_EXPORT, resources = {"event_id=#eventId"})
    @Operation(summary = "Export attendees to CSV", description = "Export attendee data to CSV format")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully exported to CSV")
    })
    public ResponseEntity<ExportResponse> exportAttendeesCSV(@PathVariable UUID eventId) {
        try {
            ExportResponse response = attendeeManagementService.exportAttendees(eventId, "csv");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/export/excel")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_EXPORT, resources = {"event_id=#eventId"})
    @Operation(summary = "Export attendees to Excel", description = "Export attendee data to Excel format")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully exported to Excel")
    })
    public ResponseEntity<ExportResponse> exportAttendeesExcel(@PathVariable UUID eventId) {
        try {
            ExportResponse response = attendeeManagementService.exportAttendees(eventId, "excel");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @PostMapping("/attendances/import/csv")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_IMPORT, resources = {"event_id=#eventId"})
    @Operation(summary = "Import attendees from CSV", description = "Import attendee data from CSV file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully imported from CSV"),
        @ApiResponse(responseCode = "400", description = "Invalid CSV data")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> importAttendeesCSV(
            @PathVariable UUID eventId,
            @RequestBody String csvData) {
        try {
            List<AttendanceDetailResponse> results = attendeeManagementService.importAttendeesCSV(eventId, csvData);
            return ResponseEntity.status(HttpStatus.CREATED).body(results);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    // Search and Filtering
    @GetMapping("/attendances/search")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Search attendees", description = "Search attendees by various criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully searched attendees")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> searchAttendees(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status) {
        try {
            List<AttendanceDetailResponse> results = attendeeManagementService.searchAttendees(eventId, name, email, status);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/filter")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Filter attendees", description = "Filter attendees by specific criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully filtered attendees")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> filterAttendees(
            @PathVariable UUID eventId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ticketType,
            @RequestParam(required = false) Boolean hasDietaryRestrictions) {
        try {
            List<AttendanceDetailResponse> results = attendeeManagementService.filterAttendees(eventId, status, ticketType, hasDietaryRestrictions);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    // Bulk Operations
    @PostMapping("/attendances/bulk-update")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_UPDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Bulk update attendees", description = "Update multiple attendees at once")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated attendees"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> bulkUpdateAttendees(
            @PathVariable UUID eventId,
            @Valid @RequestBody BulkUpdateRequest request) {
        try {
            request.setEventId(eventId);
            List<AttendanceDetailResponse> results = attendeeManagementService.bulkUpdateAttendees(eventId, request);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    @PostMapping("/attendances/bulk-delete")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_DELETE, resources = {"event_id=#eventId"})
    @Operation(summary = "Bulk delete attendees", description = "Delete multiple attendees at once")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully deleted attendees"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<Void> bulkDeleteAttendees(
            @PathVariable UUID eventId,
            @Valid @RequestBody BulkUpdateRequest request) {
        try {
            request.setEventId(eventId);
            attendeeManagementService.bulkDeleteAttendees(eventId, request);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
    
    // Health and Validation
    @GetMapping("/attendances/validate")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_VALIDATE, resources = {"event_id=#eventId"})
    @Operation(summary = "Validate attendee data", description = "Validate attendee data for completeness and accuracy")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully validated attendee data")
    })
    public ResponseEntity<Map<String, Object>> validateAttendeeData(@PathVariable UUID eventId) {
        try {
            Map<String, Object> validation = attendeeManagementService.validateAttendeeData(eventId);
            return ResponseEntity.ok(validation);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/duplicates")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Find duplicate attendees", description = "Find potential duplicate attendee records")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found duplicates")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> findDuplicateAttendees(@PathVariable UUID eventId) {
        try {
            List<AttendanceDetailResponse> duplicates = attendeeManagementService.findDuplicateAttendees(eventId);
            return ResponseEntity.ok(duplicates);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/incomplete")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Find incomplete attendee profiles", description = "Find attendees with incomplete profile information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found incomplete profiles")
    })
    public ResponseEntity<List<AttendanceDetailResponse>> findIncompleteProfiles(@PathVariable UUID eventId) {
        try {
            List<AttendanceDetailResponse> incomplete = attendeeManagementService.findIncompleteProfiles(eventId);
            return ResponseEntity.ok(incomplete);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/capacity-status")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_ANALYTICS_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get capacity status", description = "Get current capacity status for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved capacity status")
    })
    public ResponseEntity<Map<String, Object>> getCapacityStatus(@PathVariable UUID eventId) {
        try {
            Map<String, Object> status = attendeeManagementService.getCapacityStatus(eventId);
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
    
    @GetMapping("/attendances/waitlist-status")
    @RequiresPermission(value = RbacPermissions.ATTENDEE_READ, resources = {"event_id=#eventId"})
    @Operation(summary = "Get waitlist status", description = "Get current waitlist status for the event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved waitlist status")
    })
    public ResponseEntity<Map<String, Object>> getWaitlistStatus(@PathVariable UUID eventId) {
        try {
            Map<String, Object> status = attendeeManagementService.getWaitlistStatus(eventId);
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }
}
