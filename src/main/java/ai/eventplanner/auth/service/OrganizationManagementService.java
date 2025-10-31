package ai.eventplanner.auth.service;

import ai.eventplanner.auth.dto.req.OrganizationRegisterRequest;
import ai.eventplanner.auth.dto.req.OrganizationUpdateRequest;
import ai.eventplanner.auth.dto.res.OrganizationResponse;
import ai.eventplanner.auth.entity.OrganizationProfile;
import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.auth.repo.OrganizationProfileRepository;
import ai.eventplanner.auth.util.AuthMapper;
import ai.eventplanner.auth.util.OrganizationMapper;
import ai.eventplanner.common.domain.enums.OrganizationType;
import ai.eventplanner.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ai.eventplanner.auth.util.AuthValidationUtil.normalizeEmail;
import static ai.eventplanner.auth.util.AuthValidationUtil.safeTrim;

@Service
@Transactional
public class OrganizationManagementService {

    private final OrganizationProfileRepository organizationProfileRepository;

    public OrganizationManagementService(OrganizationProfileRepository organizationProfileRepository) {
        this.organizationProfileRepository = organizationProfileRepository;
    }

    public OrganizationResponse registerOrganization(UserAccount owner, OrganizationRegisterRequest request) {
        OrganizationProfile organization = OrganizationProfile.builder()
            .name(request.getName().trim())
            .description(request.getDescription())
            .type(request.getType() != null ? request.getType() : OrganizationType.CORPORATE)
            .website(safeTrim(request.getWebsite()))
            .phoneNumber(request.getPhoneNumber())
            .contactEmail(normalizeEmail(request.getContactEmail()))
            .taxId(safeTrim(request.getTaxId()))
            .registrationNumber(safeTrim(request.getRegistrationNumber()))
            .owner(owner)
            .address(OrganizationMapper.toAddress(request.getAddress()))
            .build();
        organizationProfileRepository.save(organization);
        return AuthMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse updateOrganization(UUID organizationId, UserAccount owner, OrganizationUpdateRequest request) {
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

        if (organization.getOwner() == null) {
            organization.setOwner(owner);
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
