package eventplanner.security.authorization.rbac;

/**
 * Centralized permission identifiers used throughout the controllers.
 * Keep alphabetical to reduce merge conflicts.
 */
public final class RbacPermissions {
    private RbacPermissions() {
    }

    public static final String ATTENDEE_ANALYTICS_READ = "attendee.analytics.read";
    public static final String ATTENDEE_CHECKIN = "attendee.checkin";
    public static final String ATTENDEE_CHECKOUT = "attendee.checkout";
    public static final String ATTENDEE_CREATE = "attendee.create";
    public static final String ATTENDEE_DELETE = "attendee.delete";
    public static final String ATTENDEE_EXPORT = "attendee.export";
    public static final String ATTENDEE_IMPORT = "attendee.import";
    public static final String ATTENDEE_QR_READ = "attendee.qrcode.read";
    public static final String ATTENDEE_QR_REGENERATE = "attendee.qrcode.regenerate";
    public static final String ATTENDEE_QR_SCAN = "attendee.qrcode.scan";
    public static final String ATTENDEE_READ = "attendee.read";
    public static final String ATTENDEE_UPDATE = "attendee.update";
    public static final String BUDGET_ANALYTICS_READ = "budget.analytics.read";
    public static final String BUDGET_CREATE = "budget.create";
    public static final String BUDGET_DELETE = "budget.delete";
    public static final String BUDGET_LINEITEM_CREATE = "budget.lineitem.create";
    public static final String BUDGET_LINEITEM_DELETE = "budget.lineitem.delete";
    public static final String BUDGET_LINEITEM_READ = "budget.lineitem.read";
    public static final String BUDGET_LINEITEM_UPDATE = "budget.lineitem.update";
    public static final String BUDGET_READ = "budget.read";
    public static final String BUDGET_RECALCULATE = "budget.recalculate";
    public static final String BUDGET_SUBMIT = "budget.submit";
    public static final String BUDGET_UPDATE = "budget.update";
    public static final String EVENT_ANALYTICS_READ = "event.analytics.read";
    public static final String EVENT_CAPACITY_READ = "event.capacity.read";
    public static final String EVENT_CAPACITY_UPDATE = "event.capacity.update";
    public static final String EVENT_CANCEL = "event.cancel";
    public static final String EVENT_COMPLETE = "event.complete";
    public static final String EVENT_CREATE = "event.create";
    public static final String EVENT_DELETE = "event.delete";
    public static final String EVENT_DUPLICATE = "event.duplicate";
    public static final String EVENT_HEALTH_READ = "event.health.read";
    public static final String EVENT_PUBLISH = "event.publish";
    public static final String EVENT_QR_CODE_DELETE = "event.qrcode.delete";
    public static final String EVENT_QR_CODE_GENERATE = "event.qrcode.generate";
    public static final String EVENT_QR_CODE_REGENERATE = "event.qrcode.regenerate";
    public static final String EVENT_READ = "event.read";
    public static final String EVENT_REGISTRATION_CLOSE = "event.registration.close";
    public static final String EVENT_REGISTRATION_OPEN = "event.registration.open";
    public static final String EVENT_STATUS_UPDATE = "event.update";
    public static final String EVENT_UPDATE = "event.update";
    public static final String EVENT_VALIDATION_READ = "event.validate";
    public static final String EVENT_VISIBILITY_READ = "event.visibility.read";
    public static final String EVENT_VISIBILITY_UPDATE = "event.visibility.update";
    public static final String MY_EVENTS_READ = "my.events.read";
    public static final String MY_EVENTS_SEARCH = "my.events.search";
    public static final String PUBLIC_EVENTS_SEARCH = "public.events.search";
    public static final String ROLE_ASSIGN = "role.assign";
    public static final String ROLE_REMOVE = "role.remove";
    public static final String ROLE_READ = "role.read";
    public static final String ROLE_UPDATE = "role.update";
    public static final String TIMELINE_CREATE = "timeline.create";
    public static final String TIMELINE_DELETE = "timeline.delete";
    public static final String TIMELINE_READ = "timeline.read";
    public static final String TIMELINE_TASK_CREATE = "timeline.task.create";
    public static final String TIMELINE_TASK_DELETE = "timeline.task.delete";
    public static final String TIMELINE_TASK_POSITION_UPDATE = "timeline.task.position.update";
    public static final String TIMELINE_TASK_READ = "timeline.task.read";
    public static final String TIMELINE_TASK_UPDATE = "timeline.task.update";
    public static final String TIMELINE_WORKBACK_GENERATE = "timeline.workback.generate";
    public static final String USER_READ = "user.read";
    public static final String USER_SEARCH = "user.search";
    public static final String USER_UPDATE = "user.update";
}
