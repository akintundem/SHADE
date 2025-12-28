package eventplanner.security.util;

import eventplanner.security.auth.dto.LocationDto;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.dto.res.UserSessionResponse;
import eventplanner.security.auth.dto.res.UserSettingsResponse;
import eventplanner.security.auth.entity.Location;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;
import eventplanner.security.auth.entity.UserSettings;

/**
 * Utility mappers for converting authentication domain entities to DTOs.
 */
public final class AuthMapper {

    private AuthMapper() {
    }

    /**
     * Creates a secure user response that excludes sensitive internal identifiers.
     * This should be used for all public-facing API responses.
     * Includes userId so clients can use it in subsequent requests.
     */
    public static SecureUserResponse toSecureUserResponse(UserAccount user) {
        return SecureUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .userType(user.getUserType())
                .emailVerified(user.isEmailVerified())
                .marketingOptIn(user.isMarketingOptIn())
                .profilePictureUrl(user.getProfilePictureUrl())
                .preferences(user.getPreferences())
                .settings(toUserSettingsResponse(user.getSettings()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private static UserSettingsResponse toUserSettingsResponse(UserSettings settings) {
        UserSettings effective = settings != null ? settings : UserSettings.createDefault(null);
        
        LocationDto locationDto = null;
        Location location = effective.getLocation();
        if (location != null) {
            locationDto = LocationDto.builder()
                    .locationId(location.getId())
                    .build();
        }
        
        return UserSettingsResponse.builder()
                .bio(effective.getBio())
                .location(locationDto)
                .preferredLanguage(effective.getPreferredLanguage())
                .profileVisibility(effective.getProfileVisibility())
                .eventParticipationVisibility(effective.getEventParticipationVisibility())
                .searchVisibility(Boolean.TRUE.equals(effective.getSearchVisibility()))
                .themePreference(effective.getThemePreference())
                .emailNotificationsEnabled(Boolean.TRUE.equals(effective.getEmailNotificationsEnabled()))
                .pushNotificationsEnabled(Boolean.TRUE.equals(effective.getPushNotificationsEnabled()))
                .eventInvitationsEnabled(Boolean.TRUE.equals(effective.getEventInvitationsEnabled()))
                .eventUpdatesEnabled(Boolean.TRUE.equals(effective.getEventUpdatesEnabled()))
                .eventRemindersEnabled(Boolean.TRUE.equals(effective.getEventRemindersEnabled()))
                .rsvpNotificationsEnabled(Boolean.TRUE.equals(effective.getRsvpNotificationsEnabled()))
                .commentNotificationsEnabled(Boolean.TRUE.equals(effective.getCommentNotificationsEnabled()))
                .collaborationRequestsEnabled(Boolean.TRUE.equals(effective.getCollaborationRequestsEnabled()))
                .weeklyDigestEnabled(Boolean.TRUE.equals(effective.getWeeklyDigestEnabled()))
                .activityFeedNotificationsEnabled(Boolean.TRUE.equals(effective.getActivityFeedNotificationsEnabled()))
                .autoAcceptInvitations(Boolean.TRUE.equals(effective.getAutoAcceptInvitations()))
                .exportEventDataEnabled(Boolean.TRUE.equals(effective.getExportEventDataEnabled()))
                .mfaEnabled(Boolean.TRUE.equals(effective.getMfaEnabled()))
                .reminderTimingMinutes(effective.getReminderTimingMinutes())
                .showInEventDirectory(Boolean.TRUE.equals(effective.getShowInEventDirectory()))
                .smsNotificationsEnabled(Boolean.TRUE.equals(effective.getSmsNotificationsEnabled()))
                .build();
    }

    public static UserSessionResponse toSessionResponse(UserSession session) {
        return UserSessionResponse.builder()
                .id(session.getId())
                .deviceId(session.getDeviceId())
                .ipAddress(session.getIpAddress())
                .createdAt(session.getCreatedAt())
                .lastSeenAt(session.getLastSeenAt())
                .expiresAt(session.getExpiresAt())
                .active(!session.isRevoked() && session.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                .build();
    }
}
