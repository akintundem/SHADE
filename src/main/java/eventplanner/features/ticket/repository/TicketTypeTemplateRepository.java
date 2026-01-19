package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketTypeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTypeTemplateRepository extends JpaRepository<TicketTypeTemplate, UUID> {

    List<TicketTypeTemplate> findByCreatedById(UUID createdById);
}
