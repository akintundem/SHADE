package ai.eventplanner.auth.service;

import ai.eventplanner.auth.dto.res.OrganizationResponse;
import ai.eventplanner.auth.dto.res.SecureUserResponse;
import ai.eventplanner.auth.dto.res.UserSessionResponse;
import ai.eventplanner.auth.entity.OrganizationAddress;
import ai.eventplanner.auth.entity.OrganizationProfile;
import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.entity.UserSession;

public final class AuthMapper {

    private AuthMapper() {
    }

    /**
     * Creates a secure user response that excludes sensitive internal identifiers.
     * This should be used for all public-facing API responses.
     */
    public static SecureUserResponse toSecureUserResponse(UserAccount user) {
        return SecureUserResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .userType(user.getUserType())
                .emailVerified(user.isEmailVerified())
                .marketingOptIn(user.isMarketingOptIn())
                .profileImageUrl(user.getProfileImageUrl())
                .preferences(user.getPreferences())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static OrganizationResponse toOrganizationResponse(OrganizationProfile organization) {
        OrganizationResponse.OrganizationAddressResponse addressResponse = null;
        OrganizationAddress address = organization.getAddress();
        if (address != null) {
            addressResponse = OrganizationResponse.OrganizationAddressResponse.builder()
                    .street(address.getStreet())
                    .city(address.getCity())
                    .state(address.getState())
                    .zipCode(address.getZipCode())
                    .country(address.getCountry())
                    .build();
        }

        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .type(organization.getType())
                .website(organization.getWebsite())
                .phoneNumber(organization.getPhoneNumber())
                .contactEmail(organization.getContactEmail())
                .taxId(organization.getTaxId())
                .registrationNumber(organization.getRegistrationNumber())
                .address(addressResponse)
                .ownerId(organization.getOwner() != null ? organization.getOwner().getId() : null)
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    public static UserSessionResponse toSessionResponse(UserSession session) {
        return UserSessionResponse.builder()
                .id(session.getId())
                .deviceId(session.getDeviceId())
                .clientId(session.getClientId())
                .ipAddress(session.getIpAddress())
                .createdAt(session.getCreatedAt())
                .lastSeenAt(session.getLastSeenAt())
                .expiresAt(session.getExpiresAt())
                .active(!session.isRevoked() && session.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                .build();
    }
}
