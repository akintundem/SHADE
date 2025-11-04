package eventplanner.common.communication.repository;

import eventplanner.common.communication.core.Communication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunicationRepository extends JpaRepository<Communication, UUID> {

    List<Communication> findByEventIdOrderByCreatedAtDesc(UUID eventId);
}


