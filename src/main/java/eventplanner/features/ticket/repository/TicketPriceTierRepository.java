package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketPriceTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketPriceTierRepository extends JpaRepository<TicketPriceTier, UUID> {

    @Query("SELECT t FROM TicketPriceTier t WHERE t.ticketType.id IN :ticketTypeIds")
    List<TicketPriceTier> findByTicketTypeIdIn(@Param("ticketTypeIds") List<UUID> ticketTypeIds);

    @Query("SELECT t FROM TicketPriceTier t WHERE t.ticketType.id IN :ticketTypeIds " +
           "AND (:now IS NULL OR (t.startsAt IS NULL OR t.startsAt <= :now) " +
           "AND (t.endsAt IS NULL OR t.endsAt >= :now)) " +
           "ORDER BY t.priority ASC, t.startsAt ASC NULLS FIRST")
    List<TicketPriceTier> findActiveByTicketTypeIds(@Param("ticketTypeIds") List<UUID> ticketTypeIds,
                                                    @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM TicketPriceTier t WHERE t.ticketType.id = :ticketTypeId")
    int deleteByTicketTypeId(@Param("ticketTypeId") UUID ticketTypeId);
}
