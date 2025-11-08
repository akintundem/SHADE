package eventplanner.security.auth.service;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.common.domain.enums.VendorProgramStatus;
import eventplanner.common.domain.enums.VendorTier;
import eventplanner.common.exception.ResourceNotFoundException;
import eventplanner.security.auth.dto.req.OrganizationRegisterRequest;
import eventplanner.security.auth.dto.req.OrganizationUpdateRequest;
import eventplanner.security.auth.dto.res.OrganizationResponse;
import eventplanner.security.auth.entity.OrganizationProfile;
import eventplanner.security.auth.repository.OrganizationProfileRepository;
import eventplanner.security.util.AuthMapper;
import eventplanner.security.util.OrganizationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static eventplanner.security.util.AuthValidationUtil.normalizeEmail;
import static eventplanner.security.util.AuthValidationUtil.safeTrim;

@Service
@Transactional
public class OrganizationManagementService {

    private final OrganizationProfileRepository organizationProfileRepository;

    public OrganizationManagementService(OrganizationProfileRepository organizationProfileRepository) {
        this.organizationProfileRepository = organizationProfileRepository;
    }

    public OrganizationResponse registerOrganization(OrganizationRegisterRequest request) {
        OrganizationProfile organization = null;
        String googlePlaceId = safeTrim(request.getGooglePlaceId());
        if (googlePlaceId != null && googlePlaceId.isEmpty()) {
            googlePlaceId = null;
        }
        if (googlePlaceId != null) {
            organization = organizationProfileRepository.findByGooglePlaceId(googlePlaceId).orElse(null);
        }

        if (organization == null) {
            organization = new OrganizationProfile();
        }

        organization.setName(request.getName().trim());
        organization.setDescription(request.getDescription());
        organization.setType(request.getType() != null ? request.getType() : OrganizationType.CORPORATE);
        organization.setWebsite(safeTrim(request.getWebsite()));
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setContactEmail(normalizeEmail(request.getContactEmail()));
        organization.setTaxId(safeTrim(request.getTaxId()));
        organization.setRegistrationNumber(safeTrim(request.getRegistrationNumber()));
        organization.setAddress(OrganizationMapper.toAddress(request.getAddress()));
        organization.setGooglePlaceId(googlePlaceId);
        organization.setGooglePlaceData(safeTrim(request.getGooglePlaceData()));

        boolean isJoiningProgram = request.getPlatformVendor() != null
            ? request.getPlatformVendor()
            : Boolean.TRUE; // registering via API defaults to platform vendor
        organization.setIsPlatformVendor(isJoiningProgram);

        if (organization.getId() == null) {
            VendorTier tier = request.getVendorTier() != null ? request.getVendorTier() : VendorTier.BASIC;
            organization.setVendorTier(tier);

            VendorProgramStatus status = request.getVendorStatus() != null ? request.getVendorStatus() : VendorProgramStatus.PENDING;
            organization.setVendorStatus(status);
        } else {
            if (request.getVendorTier() != null) {
                organization.setVendorTier(request.getVendorTier());
            } else if (organization.getVendorTier() == null) {
                organization.setVendorTier(VendorTier.BASIC);
            }

            if (request.getVendorStatus() != null) {
                organization.setVendorStatus(request.getVendorStatus());
            } else if (organization.getVendorStatus() == null) {
                organization.setVendorStatus(VendorProgramStatus.PENDING);
            }
        }

        if (isJoiningProgram && organization.getJoinedAt() == null) {
            organization.setJoinedAt(LocalDateTime.now());
        }

        if (request.getRating() != null) {
            organization.setRating(request.getRating());
        }
        if (request.getReviewCount() != null) {
            organization.setReviewCount(request.getReviewCount());
        }

        if (organization.getBookingCount() == null) {
            organization.setBookingCount(0);
        }

        organizationProfileRepository.save(organization);
        return AuthMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse updateOrganization(UUID organizationId, OrganizationUpdateRequest request) {
        OrganizationProfile organization = organizationProfileRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        organization.setName(request.getName().trim());
        organization.setDescription(request.getDescription());
        organization.setType(request.getType());
        organization.setWebsite(safeTrim(request.getWebsite()));
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setContactEmail(normalizeEmail(request.getContactEmail()));
        organization.setTaxId(safeTrim(request.getTaxId()));
        organization.setRegistrationNumber(safeTrim(request.getRegistrationNumber()));
        organization.setAddress(OrganizationMapper.toAddress(request.getAddress()));

        String googlePlaceId = safeTrim(request.getGooglePlaceId());
        if (googlePlaceId != null && googlePlaceId.isEmpty()) {
            googlePlaceId = null;
        }

        if (googlePlaceId != null) {
            organizationProfileRepository.findByGooglePlaceId(googlePlaceId)
                .filter(existing -> !existing.getId().equals(organizationId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Another organization is already linked to this Google Place id");
                });
        }

        organization.setGooglePlaceId(googlePlaceId);
        organization.setGooglePlaceData(safeTrim(request.getGooglePlaceData()));

        if (request.getPlatformVendor() != null) {
            organization.setIsPlatformVendor(request.getPlatformVendor());
            if (Boolean.TRUE.equals(request.getPlatformVendor()) && organization.getJoinedAt() == null) {
                organization.setJoinedAt(LocalDateTime.now());
            }
        }

        if (request.getVendorTier() != null) {
            organization.setVendorTier(request.getVendorTier());
        }

        if (request.getVendorStatus() != null) {
            organization.setVendorStatus(request.getVendorStatus());
        }

        if (request.getRating() != null) {
            organization.setRating(request.getRating());
        }

        if (request.getReviewCount() != null) {
            organization.setReviewCount(request.getReviewCount());
        }

        if (organization.getBookingCount() == null) {
            organization.setBookingCount(0);
        }

        return AuthMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse getOrganization(UUID organizationId) {
        OrganizationProfile organization = organizationProfileRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return AuthMapper.toOrganizationResponse(organization);
    }

    public Page<OrganizationResponse> searchOrganizations(String term, Pageable pageable) {
        if (term == null || term.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be empty");
        }

        String sanitized = term.trim();
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("Search term too long");
        }

        if (sanitized.matches(".*[;'\"\\\\].*")) {
            throw new IllegalArgumentException("Invalid characters in search term");
        }

        return organizationProfileRepository
            .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(sanitized, sanitized, pageable)
            .map(AuthMapper::toOrganizationResponse);
    }
}
