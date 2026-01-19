package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketTypeDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTypeDependencyRepository extends JpaRepository<TicketTypeDependency, UUID> {

    @Query("SELECT d FROM TicketTypeDependency d WHERE d.ticketType.id IN :ticketTypeIds")
    List<TicketTypeDependency> findByTicketTypeIdIn(@Param("ticketTypeIds") List<UUID> ticketTypeIds);

    @Modifying
    @Query("DELETE FROM TicketTypeDependency d WHERE d.ticketType.id = :ticketTypeId")
    int deleteByTicketTypeId(@Param("ticketTypeId") UUID ticketTypeId);
}
