package eventplanner.security.util;

import eventplanner.security.auth.dto.res.OrganizationResponse;
import eventplanner.security.auth.dto.res.SecureUserResponse;
import eventplanner.security.auth.dto.res.UserSessionResponse;
import eventplanner.security.auth.entity.OrganizationAddress;
import eventplanner.security.auth.entity.OrganizationProfile;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.entity.UserSession;

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
            .googlePlaceId(organization.getGooglePlaceId())
            .platformVendor(organization.getIsPlatformVendor())
            .vendorTier(organization.getVendorTier())
            .vendorStatus(organization.getVendorStatus())
            .joinedAt(organization.getJoinedAt())
            .rating(organization.getRating())
            .reviewCount(organization.getReviewCount())
            .bookingCount(organization.getBookingCount())
            .createdAt(organization.getCreatedAt())
            .updatedAt(organization.getUpdatedAt())
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
