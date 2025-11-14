package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.UserType;
import eventplanner.common.domain.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "auth_users",
    indexes = {
        @Index(name = "idx_auth_users_email", columnList = "email"),
        @Index(name = "idx_auth_users_name", columnList = "name")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class UserAccount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 30)
    private UserType userType;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "accept_terms", nullable = false)
    private boolean acceptTerms;

    @Column(name = "accept_privacy", nullable = false)
    private boolean acceptPrivacy;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "profile_completed", nullable = false)
    @Builder.Default
    private Boolean profileCompleted = false;

    @PrePersist
    public void onCreate() {
        if (userType == null) {
            userType = UserType.INDIVIDUAL;
        }
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        if (profileCompleted == null) {
            profileCompleted = false;
        }
    }

    @PreUpdate
    public void onUpdate() {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }
}
