package ai.eventplanner.comms.repository;

import ai.eventplanner.comms.entity.Communication;
import ai.eventplanner.comms.repo.CommunicationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Bridge repository to expose the existing CommunicationRepository to the
 * @EnableJpaRepositories package scan configured for the communication module.
 */
@Repository
public interface CommunicationRepositoryBridge extends CommunicationRepository, JpaRepository<Communication, UUID> {
}

