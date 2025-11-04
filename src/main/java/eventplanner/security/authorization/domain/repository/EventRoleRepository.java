package eventplanner.security.authorization.domain.repository;

import eventplanner.security.authorization.domain.entity.EventRole;
import eventplanner.common.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRoleRepository extends JpaRepository<EventRole, UUID> {
    
    List<EventRole> findByEventId(UUID eventId);
    
    List<EventRole> findByEventIdAndUserId(UUID eventId, UUID userId);
    
    List<EventRole> findByEventIdAndRoleName(UUID eventId, RoleName roleName);
    
    List<EventRole> findByEventIdAndIsActive(UUID eventId, Boolean isActive);
    
    Optional<EventRole> findByEventIdAndUserIdAndRoleName(UUID eventId, UUID userId, RoleName roleName);
    
    List<EventRole> findByUserId(UUID userId);
    
    @Query("SELECT er FROM EventRole er WHERE er.eventId = :eventId AND er.roleName IN :roleNames")
    List<EventRole> findByEventIdAndRoleNameIn(@Param("eventId") UUID eventId, @Param("roleNames") List<RoleName> roleNames);
    
    @Query("SELECT COUNT(er) FROM EventRole er WHERE er.eventId = :eventId AND er.roleName = :roleName AND er.isActive = true")
    Long countByEventIdAndRoleNameAndIsActive(@Param("eventId") UUID eventId, @Param("roleName") RoleName roleName);
    
    @Query("SELECT er FROM EventRole er WHERE er.eventId = :eventId AND er.assignedAt >= :startDate ORDER BY er.assignedAt ASC")
    List<EventRole> findByEventIdAndAssignedAtAfterOrderByAssignedAt(@Param("eventId") UUID eventId, @Param("startDate") java.time.LocalDateTime startDate);
}
