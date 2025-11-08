package eventplanner.features.vendor.service;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.common.domain.enums.VendorProgramStatus;
import eventplanner.common.domain.enums.VendorTier;
import eventplanner.features.vendor.dto.VendorSearchResult;
import eventplanner.features.vendor.gateway.ExternalVendorSearchClient;
import eventplanner.features.vendor.gateway.model.ExternalVendorSearchResult;
import eventplanner.security.auth.entity.OrganizationAddress;
import eventplanner.security.auth.entity.OrganizationProfile;
import eventplanner.security.auth.repository.OrganizationProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorSearchService {

    private static final String SOURCE_PLATFORM = "PLATFORM";
    private static final String SOURCE_GOOGLE = "GOOGLE";

    private final OrganizationProfileRepository organizationProfileRepository;
    private final ExternalVendorSearchClient externalVendorSearchClient;

    public List<VendorSearchResult> search(String query, OrganizationType type, String location, int limit) {
        String sanitizedQuery = query != null ? query.trim() : "";
        String sanitizedLocation = location != null ? location.trim() : "";

        List<OrganizationProfile> platformVendors = type != null
            ? organizationProfileRepository.findByIsPlatformVendorTrueAndType(type)
            : organizationProfileRepository.findByIsPlatformVendorTrue();

        List<VendorSearchResult> results = new ArrayList<>();
        Set<String> seenPlaceIds = new HashSet<>();

        for (OrganizationProfile profile : platformVendors) {
            if (!matchesQuery(profile, sanitizedQuery)) {
                continue;
            }
            if (!matchesLocation(profile, sanitizedLocation)) {
                continue;
            }

            VendorSearchResult result = toVendorSearchResult(profile);
            results.add(result);

            if (profile.getGooglePlaceId() != null) {
                seenPlaceIds.add(profile.getGooglePlaceId());
            }
        }

        int remaining = Math.max(limit - results.size(), 0);
        if (remaining > 0) {
            List<ExternalVendorSearchResult> externalResults = externalVendorSearchClient.search(
                sanitizedQuery,
                type,
                sanitizedLocation,
                remaining * 2 // fetch a bit more to allow filtering
            );

            for (ExternalVendorSearchResult external : externalResults) {
                if (external.getPlaceId() != null && seenPlaceIds.contains(external.getPlaceId())) {
                    continue; // Already represented by platform vendor
                }

                VendorSearchResult result = toVendorSearchResult(external);
                results.add(result);
                if (external.getPlaceId() != null) {
                    seenPlaceIds.add(external.getPlaceId());
                }
            }
        }

        return results.stream()
            .sorted(Comparator.comparingDouble(VendorSearchResult::getPriorityScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    private boolean matchesQuery(OrganizationProfile profile, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalized = query.toLowerCase(Locale.US);
        return (profile.getName() != null && profile.getName().toLowerCase(Locale.US).contains(normalized))
            || (profile.getDescription() != null && profile.getDescription().toLowerCase(Locale.US).contains(normalized))
            || (profile.getWebsite() != null && profile.getWebsite().toLowerCase(Locale.US).contains(normalized));
    }

    private boolean matchesLocation(OrganizationProfile profile, String location) {
        if (!StringUtils.hasText(location)) {
            return true;
        }

        OrganizationAddress address = profile.getAddress();
        if (address == null) {
            return false;
        }

        String normalized = location.toLowerCase(Locale.US);
        return matches(address.getCity(), normalized)
            || matches(address.getState(), normalized)
            || matches(address.getCountry(), normalized);
    }

    private boolean matches(String value, String normalizedLocation) {
        return value != null && value.toLowerCase(Locale.US).contains(normalizedLocation);
    }

    private VendorSearchResult toVendorSearchResult(OrganizationProfile profile) {
        OrganizationAddress address = profile.getAddress();

        return VendorSearchResult.builder()
            .organizationId(profile.getId())
            .googlePlaceId(profile.getGooglePlaceId())
            .source(SOURCE_PLATFORM)
            .name(profile.getName())
            .description(profile.getDescription())
            .type(profile.getType())
            .website(profile.getWebsite())
            .phoneNumber(profile.getPhoneNumber())
            .email(profile.getContactEmail())
            .city(address != null ? address.getCity() : null)
            .state(address != null ? address.getState() : null)
            .country(address != null ? address.getCountry() : null)
            .platformVendor(Boolean.TRUE.equals(profile.getIsPlatformVendor()))
            .vendorTier(profile.getVendorTier())
            .vendorStatus(profile.getVendorStatus())
            .joinedAt(profile.getJoinedAt())
            .rating(profile.getRating())
            .reviewCount(profile.getReviewCount())
            .bookingCount(Optional.ofNullable(profile.getBookingCount()).orElse(0))
            .priorityScore(calculatePlatformPriorityScore(profile))
            .build();
    }

    private VendorSearchResult toVendorSearchResult(ExternalVendorSearchResult external) {
        return VendorSearchResult.builder()
            .organizationId(null)
            .googlePlaceId(external.getPlaceId())
            .source(SOURCE_GOOGLE)
            .name(external.getName())
            .description(external.getDescription())
            .type(null)
            .website(external.getWebsite())
            .phoneNumber(external.getPhoneNumber())
            .email(external.getEmail())
            .city(external.getCity())
            .state(external.getState())
            .country(external.getCountry())
            .platformVendor(false)
            .vendorTier(null)
            .vendorStatus(null)
            .joinedAt(null)
            .rating(external.getRating())
            .reviewCount(external.getReviewCount())
            .bookingCount(0)
            .priorityScore(calculateExternalPriorityScore(external))
            .build();
    }

    private double calculatePlatformPriorityScore(OrganizationProfile profile) {
        double base = Optional.ofNullable(profile.getRating()).orElse(4.0);
        int bookings = Optional.ofNullable(profile.getBookingCount()).orElse(0);
        double bookingBoost = Math.log(bookings + 1);

        VendorTier tier = profile.getVendorTier();
        double tierBoost = switch (tier != null ? tier : VendorTier.BASIC) {
            case BASIC -> 0.5;
            case PREMIUM -> 1.0;
            case PARTNER -> 1.5;
        };

        VendorProgramStatus status = profile.getVendorStatus();
        double statusBoost = VendorProgramStatus.APPROVED.equals(status) ? 1.0 : 0.0;

        return base + bookingBoost + tierBoost + statusBoost;
    }

    private double calculateExternalPriorityScore(ExternalVendorSearchResult external) {
        Double rating = external.getRating();
        if (rating == null) {
            rating = 3.5;
        }
        return rating * 0.5;
    }
}

