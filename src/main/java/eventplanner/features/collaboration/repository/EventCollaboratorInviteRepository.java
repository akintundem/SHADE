package eventplanner.features.collaboration.repository;

import eventplanner.features.collaboration.entity.EventCollaboratorInvite;
import eventplanner.features.collaboration.enums.CollaboratorInviteStatus;
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
public interface EventCollaboratorInviteRepository extends JpaRepository<EventCollaboratorInvite, UUID> {

    Page<EventCollaboratorInvite> findByEventId(UUID eventId, Pageable pageable);

    List<EventCollaboratorInvite> findByEventId(UUID eventId);

    @Query("SELECT i FROM EventCollaboratorInvite i WHERE i.invitee.id = :inviteeUserId AND i.status = :status ORDER BY i.createdAt DESC")
    List<EventCollaboratorInvite> findByInviteeUserIdAndStatusOrderByCreatedAtDesc(@Param("inviteeUserId") UUID inviteeUserId, @Param("status") CollaboratorInviteStatus status);

    @Query("SELECT i FROM EventCollaboratorInvite i WHERE i.event.id = :eventId AND i.invitee.id = :inviteeUserId AND i.status = :status ORDER BY i.createdAt DESC")
    Optional<EventCollaboratorInvite> findFirstByEventIdAndInviteeUserIdAndStatusOrderByCreatedAtDesc(
            @Param("eventId") UUID eventId, @Param("inviteeUserId") UUID inviteeUserId, @Param("status") CollaboratorInviteStatus status
    );

    Optional<EventCollaboratorInvite> findFirstByEventIdAndInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            UUID eventId, String inviteeEmail, CollaboratorInviteStatus status
    );

    Optional<EventCollaboratorInvite> findByTokenHash(String tokenHash);

    @Query("""
            SELECT i
            FROM EventCollaboratorInvite i
            WHERE (i.invitee.id = :userId OR (i.inviteeEmail IS NOT NULL AND LOWER(i.inviteeEmail) = LOWER(:email)))
              AND i.status = :status
            ORDER BY i.createdAt DESC
            """)
    List<EventCollaboratorInvite> findIncomingInvites(
            @Param("userId") UUID userId,
            @Param("email") String email,
            @Param("status") CollaboratorInviteStatus status
    );

    /**
     * Paginated version of incoming invites that also excludes expired ones.
     */
    @Query("""
            SELECT i
            FROM EventCollaboratorInvite i
            WHERE (i.invitee.id = :userId OR (i.inviteeEmail IS NOT NULL AND LOWER(i.inviteeEmail) = LOWER(:email)))
              AND i.status = :status
              AND (i.expiresAt IS NULL OR i.expiresAt > :now)
            ORDER BY i.createdAt DESC
            """)
    Page<EventCollaboratorInvite> findIncomingInvitesPaginated(
            @Param("userId") UUID userId,
            @Param("email") String email,
            @Param("status") CollaboratorInviteStatus status,
            @Param("now") java.time.LocalDateTime now,
            Pageable pageable
    );
}

