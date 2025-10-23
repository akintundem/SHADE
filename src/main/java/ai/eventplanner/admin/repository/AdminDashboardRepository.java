package ai.eventplanner.admin.repository;

import ai.eventplanner.admin.entity.AdminDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for admin dashboard
 */
@Repository
public interface AdminDashboardRepository extends JpaRepository<AdminDashboard, UUID> {
    
    /**
     * Find the most recent dashboard entry
     */
    Optional<AdminDashboard> findFirstByOrderByCreatedAtDesc();
}
