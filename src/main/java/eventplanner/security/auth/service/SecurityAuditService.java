package eventplanner.security.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.security.auth.entity.SecurityAuditLog;
import eventplanner.security.auth.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for logging security events to audit trail.
 * Provides comprehensive security event logging for compliance and monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Log a security event asynchronously to avoid blocking the main request flow.
     */
    @Async
    @Transactional
    public void logSecurityEvent(SecurityAuditLog.SecurityEventType eventType,
                                 SecurityAuditLog.SecurityEventStatus status,
                                 String message,
                                 UUID userId,
                                 String email,
                                 String ipAddress,
                                 String userAgent,
                                 String deviceId,
                                 String riskLevel,
                                 UUID sessionId,
                                 Map<String, Object> metadata) {
        try {
            String metadataJson = null;
            if (metadata != null && !metadata.isEmpty()) {
                try {
                    metadataJson = objectMapper.writeValueAsString(metadata);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize metadata for audit log: {}", e.getMessage());
                }
            }

            SecurityAuditLog auditLog = SecurityAuditLog.builder()
                    .eventType(eventType)
                    .status(status)
                    .message(message)
                    .userId(userId)
                    .email(email)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .deviceId(deviceId)
                    .riskLevel(riskLevel)
                    .sessionId(sessionId)
                    .metadata(metadataJson)
                    .occurredAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Security audit log saved: {} - {}", eventType, status);

        } catch (Exception e) {
            // Never fail the main request due to audit logging issues
            log.error("Failed to save security audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Log login attempt (success or failure)
     */
    public void logLoginAttempt(boolean success,
                                UUID userId,
                                String email,
                                String ipAddress,
                                String userAgent,
                                String deviceId,
                                String failureReason) {
        SecurityAuditLog.SecurityEventType eventType = success 
            ? SecurityAuditLog.SecurityEventType.LOGIN_SUCCESS 
            : SecurityAuditLog.SecurityEventType.LOGIN_FAILURE;
        
        SecurityAuditLog.SecurityEventStatus status = success 
            ? SecurityAuditLog.SecurityEventStatus.SUCCESS 
            : SecurityAuditLog.SecurityEventStatus.FAILURE;

        String message = success 
            ? "User logged in successfully" 
            : "Login attempt failed: " + (failureReason != null ? failureReason : "Invalid credentials");

        Map<String, Object> metadata = new HashMap<>();
        if (failureReason != null) {
            metadata.put("failureReason", failureReason);
        }

        logSecurityEvent(eventType, status, message, userId, email, ipAddress, userAgent, 
                        deviceId, success ? "LOW" : "MEDIUM", null, metadata);
    }

    /**
     * Log account lockout event
     */
    public void logAccountLocked(UUID userId, String email, String ipAddress, int failedAttempts, long lockoutDurationMinutes) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("failedAttempts", failedAttempts);
        metadata.put("lockoutDurationMinutes", lockoutDurationMinutes);

        logSecurityEvent(
            SecurityAuditLog.SecurityEventType.ACCOUNT_LOCKED,
            SecurityAuditLog.SecurityEventStatus.BLOCKED,
            String.format("Account locked due to %d failed login attempts. Lockout duration: %d minutes", 
                         failedAttempts, lockoutDurationMinutes),
            userId,
            email,
            ipAddress,
            null,
            null,
            "HIGH",
            null,
            metadata
        );
    }

    /**
     * Log rate limit exceeded event
     */
    public void logRateLimitExceeded(String email, String ipAddress, String endpoint, String rateLimitType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("endpoint", endpoint);
        metadata.put("rateLimitType", rateLimitType);

        logSecurityEvent(
            SecurityAuditLog.SecurityEventType.RATE_LIMIT_EXCEEDED,
            SecurityAuditLog.SecurityEventStatus.BLOCKED,
            String.format("Rate limit exceeded for %s on endpoint %s", rateLimitType, endpoint),
            null,
            email,
            ipAddress,
            null,
            null,
            "MEDIUM",
            null,
            metadata
        );
    }

    /**
     * Log registration event
     */
    public void logRegistration(boolean success, UUID userId, String email, String ipAddress, String failureReason) {
        SecurityAuditLog.SecurityEventType eventType = success 
            ? SecurityAuditLog.SecurityEventType.REGISTRATION_SUCCESS 
            : SecurityAuditLog.SecurityEventType.REGISTRATION;
        
        SecurityAuditLog.SecurityEventStatus status = success 
            ? SecurityAuditLog.SecurityEventStatus.SUCCESS 
            : SecurityAuditLog.SecurityEventStatus.FAILURE;

        String message = success 
            ? "User registered successfully" 
            : "Registration failed: " + (failureReason != null ? failureReason : "Unknown error");

        Map<String, Object> metadata = new HashMap<>();
        if (failureReason != null) {
            metadata.put("failureReason", failureReason);
        }

        logSecurityEvent(eventType, status, message, userId, email, ipAddress, null, 
                        null, success ? "LOW" : "MEDIUM", null, metadata);
    }

    /**
     * Log password change event
     */
    public void logPasswordChange(UUID userId, String email, String ipAddress, boolean success) {
        logSecurityEvent(
            SecurityAuditLog.SecurityEventType.PASSWORD_CHANGE,
            success ? SecurityAuditLog.SecurityEventStatus.SUCCESS : SecurityAuditLog.SecurityEventStatus.FAILURE,
            success ? "Password changed successfully" : "Password change failed",
            userId,
            email,
            ipAddress,
            null,
            null,
            success ? "LOW" : "HIGH",
            null,
            null
        );
    }

    /**
     * Log email verification event
     */
    public void logEmailVerification(UUID userId, String email, String ipAddress, boolean success) {
        logSecurityEvent(
            SecurityAuditLog.SecurityEventType.EMAIL_VERIFICATION_SUCCESS,
            success ? SecurityAuditLog.SecurityEventStatus.SUCCESS : SecurityAuditLog.SecurityEventStatus.FAILURE,
            success ? "Email verified successfully" : "Email verification failed",
            userId,
            email,
            ipAddress,
            null,
            null,
            "LOW",
            null,
            null
        );
    }
}

