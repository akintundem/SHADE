package eventplanner.security.auth.repository;

import eventplanner.common.domain.enums.OrganizationType;
import eventplanner.security.auth.entity.OrganizationProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, UUID> {
    Page<OrganizationProfile> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);

    Optional<OrganizationProfile> findByGooglePlaceId(String googlePlaceId);

    boolean existsByGooglePlaceId(String googlePlaceId);

    List<OrganizationProfile> findByIsPlatformVendorTrueAndType(OrganizationType type);

    List<OrganizationProfile> findByIsPlatformVendorTrue();
}
