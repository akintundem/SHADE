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
    
    /**
     * Find all ticket types for an event.
     */
    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId")
    List<TicketType> findByEventId(@Param("eventId") UUID eventId);
    
    /**
     * Find active ticket types for an event.
     */
    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.isActive = true")
    List<TicketType> findByEventIdAndIsActiveTrue(@Param("eventId") UUID eventId);
    
    /**
     * Find ticket type by ID and event ID (for validation).
     */
    @Query("SELECT t FROM TicketType t WHERE t.id = :id AND t.event.id = :eventId")
    Optional<TicketType> findByIdAndEventId(@Param("id") UUID id, @Param("eventId") UUID eventId);
    
    /**
     * Find ticket type with pessimistic lock for atomic updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findByIdForUpdate(@Param("id") UUID id);
    
    /**
     * Count active ticket types for an event.
     */
    @Query("SELECT COUNT(t) FROM TicketType t WHERE t.event.id = :eventId AND t.isActive = true")
    int countActiveByEventId(@Param("eventId") UUID eventId);
    
    /**
     * Atomically increment quantity sold.
     */
    @Modifying
    @Query("UPDATE TicketType t SET t.quantitySold = t.quantitySold + :quantity WHERE t.id = :id")
    int incrementQuantitySold(@Param("id") UUID id, @Param("quantity") int quantity);
    
    /**
     * Atomically increment quantity reserved.
     */
    @Modifying
    @Query("UPDATE TicketType t SET t.quantityReserved = t.quantityReserved + :quantity WHERE t.id = :id")
    int incrementQuantityReserved(@Param("id") UUID id, @Param("quantity") int quantity);
    
    /**
     * Atomically decrement quantity reserved.
     */
    @Modifying
    @Query("UPDATE TicketType t SET t.quantityReserved = t.quantityReserved - :quantity WHERE t.id = :id")
    int decrementQuantityReserved(@Param("id") UUID id, @Param("quantity") int quantity);
    
    /**
     * Move reserved tickets to sold (atomic operation).
     */
    @Modifying
    @Query("UPDATE TicketType t SET t.quantityReserved = t.quantityReserved - :quantity, t.quantitySold = t.quantitySold + :quantity WHERE t.id = :id")
    int moveReservedToSold(@Param("id") UUID id, @Param("quantity") int quantity);

    /**
     * Find ticket types by category for an event.
     */
    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.category = :category")
    List<TicketType> findByEventIdAndCategory(@Param("eventId") UUID eventId, @Param("category") TicketTypeCategory category);

    /**
     * Find active ticket types by category for an event.
     */
    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.category = :category AND t.isActive = true")
    List<TicketType> findByEventIdAndCategoryAndIsActive(@Param("eventId") UUID eventId, @Param("category") TicketTypeCategory category);
}

