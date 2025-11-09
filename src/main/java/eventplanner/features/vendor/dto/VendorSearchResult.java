package eventplanner.features.vendor.dto;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.common.domain.enums.VendorProgramStatus;
import eventplanner.common.domain.enums.VendorTier;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class VendorSearchResult {
    UUID organizationId;
    String googlePlaceId;
    String source; // PLATFORM or GOOGLE
    String name;
    String description;
    OrganizationType type;
    String website;
    String phoneNumber;
    String email;
    String city;
    String state;
    String country;
    boolean platformVendor;
    VendorTier vendorTier;
    VendorProgramStatus vendorStatus;
    LocalDateTime joinedAt;
    Double rating;
    Integer reviewCount;
    Integer bookingCount;
    double priorityScore;
}


