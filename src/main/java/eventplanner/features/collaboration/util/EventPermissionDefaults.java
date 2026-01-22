package eventplanner.features.collaboration.util;

import eventplanner.features.collaboration.enums.EventPermission;
import eventplanner.features.event.enums.EventUserType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Default permissions applied when a collaborator has no explicit overrides.
 */
public final class EventPermissionDefaults {

    private static final EnumSet<EventPermission> VIEW_ONLY =
            EnumSet.of(EventPermission.VIEW_EVENT);
    private static final EnumSet<EventPermission> SCHEDULE_MANAGER =
            EnumSet.of(EventPermission.VIEW_EVENT, EventPermission.MANAGE_SCHEDULE);
    private static final EnumSet<EventPermission> CONTENT_MANAGER =
            EnumSet.of(EventPermission.VIEW_EVENT, EventPermission.MANAGE_CONTENT);
    private static final EnumSet<EventPermission> FULL =
            EnumSet.allOf(EventPermission.class);

    private static final Map<EventUserType, EnumSet<EventPermission>> DEFAULTS = buildDefaults();

    private EventPermissionDefaults() {
    }

    public static Set<EventPermission> defaultsForRole(EventUserType role) {
        if (role == null) {
            return VIEW_ONLY;
        }
        EnumSet<EventPermission> defaults = DEFAULTS.get(role);
        return defaults != null ? defaults : VIEW_ONLY;
    }

    private static Map<EventUserType, EnumSet<EventPermission>> buildDefaults() {
        Map<EventUserType, EnumSet<EventPermission>> defaults = new EnumMap<>(EventUserType.class);

        defaults.put(EventUserType.ADMIN, FULL);
        defaults.put(EventUserType.ORGANIZER, FULL);
        defaults.put(EventUserType.COORDINATOR, FULL);

        defaults.put(EventUserType.COLLABORATOR, SCHEDULE_MANAGER);
        defaults.put(EventUserType.STAFF, SCHEDULE_MANAGER);
        defaults.put(EventUserType.VOLUNTEER, SCHEDULE_MANAGER);

        defaults.put(EventUserType.MEDIA, CONTENT_MANAGER);

        defaults.put(EventUserType.SPEAKER, VIEW_ONLY);
        defaults.put(EventUserType.SPONSOR, VIEW_ONLY);
        defaults.put(EventUserType.ATTENDEE, VIEW_ONLY);

        return defaults;
    }
}
