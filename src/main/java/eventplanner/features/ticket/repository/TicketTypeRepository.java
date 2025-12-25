package eventplanner.features.ticket.repository;

import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TicketType entity with atomic quantity update support.
 */
@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {

    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId")
    List<TicketType> findByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.isActive = true")
    List<TicketType> findByEventIdAndIsActiveTrue(@Param("eventId") UUID eventId);

    @Query("SELECT t FROM TicketType t WHERE t.id = :id AND t.event.id = :eventId")
    Optional<TicketType> findByIdAndEventId(@Param("id") UUID id, @Param("eventId") UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE TicketType t SET t.quantitySold = t.quantitySold + :quantity WHERE t.id = :id")
    int incrementQuantitySold(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE TicketType t SET t.quantityReserved = t.quantityReserved + :quantity WHERE t.id = :id")
    int incrementQuantityReserved(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE TicketType t SET t.quantityReserved = t.quantityReserved - :quantity WHERE t.id = :id")
    int decrementQuantityReserved(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE TicketType t SET t.quantityReserved = t.quantityReserved - :quantity, t.quantitySold = t.quantitySold + :quantity WHERE t.id = :id")
    int moveReservedToSold(@Param("id") UUID id, @Param("quantity") int quantity);

    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.category = :category")
    List<TicketType> findByEventIdAndCategory(@Param("eventId") UUID eventId, @Param("category") TicketTypeCategory category);

    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.category = :category AND t.isActive = true")
    List<TicketType> findByEventIdAndCategoryAndIsActive(@Param("eventId") UUID eventId, @Param("category") TicketTypeCategory category);
}
