package ai.eventplanner.auth.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.OrganizationType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "auth_organizations",
    indexes = {
        @Index(name = "idx_auth_org_name", columnList = "name"),
        @Index(name = "idx_auth_org_type", columnList = "type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class OrganizationProfile extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private OrganizationType type;

    @Column(name = "website", length = 200)
    private String website;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "tax_id", length = 40)
    private String taxId;

    @Column(name = "registration_number", length = 60)
    private String registrationNumber;

    @Embedded
    private OrganizationAddress address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;
}
