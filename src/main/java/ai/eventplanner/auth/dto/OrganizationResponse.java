package ai.eventplanner.auth.dto;

import ai.eventplanner.common.domain.enums.OrganizationType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class OrganizationResponse {
    UUID id;
    String name;
    String description;
    OrganizationType type;
    String website;
    String phoneNumber;
    String contactEmail;
    String taxId;
    String registrationNumber;
    OrganizationAddressResponse address;
    UUID ownerId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Value
    @Builder
    public static class OrganizationAddressResponse {
        String street;
        String city;
        String state;
        String zipCode;
        String country;
    }
}
