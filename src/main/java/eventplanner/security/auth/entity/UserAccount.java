package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.security.auth.enums.UserType;
import eventplanner.security.auth.enums.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(
    name = "auth_users",
    indexes = {
        @Index(name = "idx_auth_users_email", columnList = "email"),
        @Index(name = "idx_auth_users_name", columnList = "name"),
        @Index(name = "idx_auth_users_username", columnList = "username")
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

    @Column(name = "cognito_sub", unique = true, length = 120)
    private String cognitoSub;

    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Public handle / username (lowercased). Nullable for backwards compatibility
     * and for accounts that haven't completed onboarding yet.
     */
    @Column(name = "username", unique = true, length = 40)
    private String username;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 30)
    private UserType userType;

    @Column(name = "accept_terms", nullable = false)
    private boolean acceptTerms;

    @Column(name = "accept_privacy", nullable = false)
    private boolean acceptPrivacy;

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @Column(name = "profile_completed", nullable = false)
    @Builder.Default
    private Boolean profileCompleted = false;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserSettings settings;

    @PrePersist
    public void onCreate() {
        if (userType == null) {
            userType = UserType.INDIVIDUAL;
        }
        if (status == null) {
            status = UserStatus.ACTIVE;
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
