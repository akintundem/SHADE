package eventplanner.features.collaboration.repository;

import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.common.domain.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventUserRepository extends JpaRepository<EventUser, UUID> {
    
    List<EventUser> findByEventId(UUID eventId);
    Page<EventUser> findByEventId(UUID eventId, Pageable pageable);
    
    List<EventUser> findByEventIdAndUserType(UUID eventId, EventUserType userType);
    
    List<EventUser> findByEventIdAndRegistrationStatus(UUID eventId, RegistrationStatus status);
    
    Optional<EventUser> findByEventIdAndUserId(UUID eventId, UUID userId);
    
    List<EventUser> findByUserId(UUID userId);
    
    @Query("SELECT eu FROM EventUser eu WHERE eu.eventId = :eventId AND eu.userType IN :userTypes")
    List<EventUser> findByEventIdAndUserTypeIn(@Param("eventId") UUID eventId, @Param("userTypes") List<EventUserType> userTypes);
    
    @Query("SELECT COUNT(eu) FROM EventUser eu WHERE eu.eventId = :eventId AND eu.userType = :userType")
    Long countByEventIdAndUserType(@Param("eventId") UUID eventId, @Param("userType") EventUserType userType);
    
    @Query("SELECT eu FROM EventUser eu WHERE eu.eventId = :eventId AND eu.registrationDate >= :startDate ORDER BY eu.registrationDate ASC")
    List<EventUser> findByEventIdAndRegistrationDateAfterOrderByRegistrationDate(@Param("eventId") UUID eventId, @Param("startDate") java.time.LocalDateTime startDate);
}
