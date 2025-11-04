package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.ClientApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientApplicationRepository extends JpaRepository<ClientApplication, String> {

    /**
     * Find active client application by client ID
     */
    @Query("SELECT c FROM ClientApplication c WHERE c.clientId = :clientId AND c.active = true")
    Optional<ClientApplication> findActiveByClientId(@Param("clientId") String clientId);

    /**
     * Check if client ID exists and is active
     */
    @Query("SELECT COUNT(c) > 0 FROM ClientApplication c WHERE c.clientId = :clientId AND c.active = true")
    boolean existsActiveByClientId(@Param("clientId") String clientId);

    /**
     * Find by client ID (including inactive)
     */
    Optional<ClientApplication> findByClientId(String clientId);
}
