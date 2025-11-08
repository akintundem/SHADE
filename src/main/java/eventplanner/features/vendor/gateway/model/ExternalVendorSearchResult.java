package eventplanner.features.vendor.gateway.model;

import lombok.Builder;
import lombok.Value;

/**
 * Lightweight representation of an external vendor search result (e.g. Google Places).
 */
@Value
@Builder
public class ExternalVendorSearchResult {
    String placeId;
    String name;
    String description;
    String website;
    String phoneNumber;
    String email;
    String city;
    String state;
    String country;
    Double rating;
    Integer reviewCount;
    String rawJsonPayload;
}

