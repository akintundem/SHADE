package eventplanner.common.communication.repository;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.enums.CommunicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunicationRepository extends JpaRepository<Communication, UUID> {

    List<Communication> findByEventIdOrderByCreatedAtDesc(UUID eventId);
    
    Optional<Communication> findFirstByRecipientEmailAndTemplateIdAndSubjectAndStatusOrderByCreatedAtDesc(
            String recipientEmail, String templateId, String subject, CommunicationStatus status);
}


