package eventplanner.security.authorization.domain.repository;

import eventplanner.security.authorization.domain.entity.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationRoleRepository extends JpaRepository<OrganizationRole, UUID> {
    
    List<OrganizationRole> findByUserId(UUID userId);
    
    List<OrganizationRole> findByOrganizationId(UUID organizationId);
    
    @Query("SELECT or FROM OrganizationRole or WHERE or.userId = :userId AND or.organizationId = :organizationId AND or.isActive = true")
    List<OrganizationRole> findByUserIdAndOrganizationIdAndActive(@Param("userId") UUID userId, @Param("organizationId") UUID organizationId);
    
    @Query("SELECT or FROM OrganizationRole or WHERE or.userId = :userId AND or.isActive = true")
    List<OrganizationRole> findByUserIdAndActive(@Param("userId") UUID userId);
    
    @Query("SELECT or FROM OrganizationRole or WHERE or.organizationId = :organizationId AND or.role = :role AND or.isActive = true")
    List<OrganizationRole> findByOrganizationIdAndRoleAndActive(@Param("organizationId") UUID organizationId, @Param("role") String role);
    
    @Query("SELECT COUNT(or) FROM OrganizationRole or WHERE or.organizationId = :organizationId AND or.role = 'OWNER' AND or.isActive = true")
    Long countOwnersByOrganizationId(@Param("organizationId") UUID organizationId);
}
