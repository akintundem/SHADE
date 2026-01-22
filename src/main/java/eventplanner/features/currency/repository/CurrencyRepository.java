package eventplanner.features.currency.repository;

import eventplanner.features.currency.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Currency entity.
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

    /**
     * Find all active currencies.
     */
    @Query("SELECT c FROM Currency c WHERE c.isActive = true ORDER BY c.code")
    List<Currency> findAllActive();

    /**
     * Find currency by code (case-insensitive).
     */
    @Query("SELECT c FROM Currency c WHERE UPPER(c.code) = UPPER(:code)")
    Currency findByCodeIgnoreCase(String code);
}
