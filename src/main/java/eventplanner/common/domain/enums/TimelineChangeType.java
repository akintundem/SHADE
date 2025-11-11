package eventplanner.common.domain.enums;

/**
 * Represents major timeline change types captured in the audit log.
 */
public enum TimelineChangeType {
    CREATED,
    UPDATED,
    DELETED,
    REORDERED,
    DEPENDENCIES_UPDATED,
    PUBLISHED,
    UNPUBLISHED
}

