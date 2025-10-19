package ai.eventplanner.auth.repo;

import ai.eventplanner.auth.entity.OrganizationProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, UUID> {
    Page<OrganizationProfile> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
}
