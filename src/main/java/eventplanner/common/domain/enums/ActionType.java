package eventplanner.common.domain.enums;

/**
 * Unified action types for all audit logs across all domains
 * Consolidates actions from Security, Attendee, Budget, and other modules
 */
public enum ActionType {
    // Generic CRUD operations
    CREATE,
    READ,
    UPDATE,
    DELETE,
    
    // Generic management actions
    MANAGE,
    ASSIGN,
    REMOVE,
    PUBLISH,
    ARCHIVE,
    RESTORE,
    
    // Approval workflow actions
    SUBMIT,
    APPROVE,
    REJECT,
    
    // Security/Authentication actions
    LOGIN,
    LOGIN_ATTEMPT,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    REGISTRATION,
    REGISTRATION_SUCCESS,
    PASSWORD_CHANGE,
    PASSWORD_RESET_REQUEST,
    PASSWORD_RESET_SUCCESS,
    EMAIL_VERIFICATION,
    EMAIL_VERIFICATION_SUCCESS,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    RATE_LIMIT_EXCEEDED,
    TOKEN_REFRESH,
    TOKEN_VALIDATION,
    SUSPICIOUS_ACTIVITY,
    PERMISSION_DENIED,
    SESSION_CREATED,
    SESSION_REVOKED,
    
    // Attendee-specific actions
    RSVP_UPDATED,
    CHECK_IN,
    CHECK_OUT,
    INVITATION_QUEUED,
    INVITATION_SENT,
    INVITATION_FAILED,
    BULK_IMPORT,
    BULK_UPDATE,
    BULK_DELETE,
    EXPORT,
    
    // Budget-specific actions (already have SUBMIT, APPROVE, REJECT above)
    RECALCULATE,
    
    // Event-specific actions
    EVENT_CANCELLED,
    EVENT_RESCHEDULED,
    
    // Contract and payment actions
    CONTRACT_SIGNED,
    PAYMENT_MADE
}
