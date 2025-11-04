package eventplanner.common.communication.repository;

import eventplanner.common.communication.model.CommunicationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunicationLogRepository extends JpaRepository<CommunicationLogEntity, UUID> {
}
