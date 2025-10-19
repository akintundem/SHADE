package ai.eventplanner.comms.repository;

import ai.eventplanner.comms.model.CommunicationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunicationLogRepository extends JpaRepository<CommunicationLogEntity, UUID> {
}
