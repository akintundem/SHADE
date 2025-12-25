package eventplanner.features.collaboration.repository;

import eventplanner.features.collaboration.entity.EventUser;
import eventplanner.common.domain.enums.EventUserType;
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

    @Query("SELECT eu FROM EventUser eu WHERE eu.event.id = :eventId")
    List<EventUser> findByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT eu FROM EventUser eu WHERE eu.event.id = :eventId")
    Page<EventUser> findByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT eu FROM EventUser eu WHERE eu.event.id = :eventId AND eu.user.id = :userId")
    Optional<EventUser> findByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);

    @Query("SELECT eu FROM EventUser eu WHERE eu.user.id = :userId")
    List<EventUser> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT eu FROM EventUser eu WHERE eu.event.id = :eventId AND eu.userType IN :userTypes")
    List<EventUser> findByEventIdAndUserTypeIn(@Param("eventId") UUID eventId, @Param("userTypes") List<EventUserType> userTypes);
}
