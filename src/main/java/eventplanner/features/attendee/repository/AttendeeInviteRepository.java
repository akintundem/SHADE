package eventplanner.features.attendee.repository;

import eventplanner.features.attendee.entity.AttendeeInvite;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendeeInviteRepository extends JpaRepository<AttendeeInvite, UUID> {

    Page<AttendeeInvite> findByEventId(UUID eventId, Pageable pageable);

    Page<AttendeeInvite> findByEventIdAndStatus(UUID eventId, AttendeeInviteStatus status, Pageable pageable);

    List<AttendeeInvite> findByEventId(UUID eventId);

    Optional<AttendeeInvite> findByIdAndEventId(UUID inviteId, UUID eventId);

    Optional<AttendeeInvite> findByTokenHash(String tokenHash);

    Optional<AttendeeInvite> findFirstByEventIdAndInviteeIdAndStatusOrderByCreatedAtDesc(
            UUID eventId,
            UUID inviteeUserId,
            AttendeeInviteStatus status
    );

    Optional<AttendeeInvite> findFirstByEventIdAndInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            UUID eventId,
            String inviteeEmail,
            AttendeeInviteStatus status
    );

    boolean existsByEventIdAndInviteeIdAndStatus(UUID eventId, UUID inviteeUserId, AttendeeInviteStatus status);

    boolean existsByEventIdAndInviteeEmailIgnoreCaseAndStatus(UUID eventId, String inviteeEmail, AttendeeInviteStatus status);

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

    @Query("""
            SELECT i
            FROM AttendeeInvite i
            WHERE i.status = 'PENDING'
              AND i.expiresAt IS NOT NULL
              AND i.expiresAt < :now
            """)
    List<AttendeeInvite> findExpiredPendingInvites(@Param("now") LocalDateTime now);
}
