package eventplanner.features.attendee.repository;

import eventplanner.features.attendee.entity.AttendeeInvite;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
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
public interface AttendeeInviteRepository extends JpaRepository<AttendeeInvite, UUID> {

    Page<AttendeeInvite> findByEventId(UUID eventId, Pageable pageable);

    List<AttendeeInvite> findByEventId(UUID eventId);

    Optional<AttendeeInvite> findByTokenHash(String tokenHash);

    @Query("""
            SELECT i
            FROM AttendeeInvite i
            WHERE (i.invitee.id = :userId OR (i.inviteeEmail IS NOT NULL AND LOWER(i.inviteeEmail) = LOWER(:email)))
              AND i.status = :status
            ORDER BY i.createdAt DESC
            """)
    List<AttendeeInvite> findIncomingInvites(
            @Param("userId") UUID userId,
            @Param("email") String email,
            @Param("status") AttendeeInviteStatus status
    );
}
