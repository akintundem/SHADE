package ai.eventplanner.auth.repo;

import ai.eventplanner.auth.entity.UserAccount;
import ai.eventplanner.common.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Page<UserAccount> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
    
    @Query("SELECT COUNT(u) FROM UserAccount u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT u.status, COUNT(u) FROM UserAccount u GROUP BY u.status")
    Map<String, Long> getUsersByStatus();
    
    @Query("SELECT DATE(u.createdAt) as date, COUNT(u) as count FROM UserAccount u WHERE u.createdAt BETWEEN :startDate AND :endDate GROUP BY DATE(u.createdAt) ORDER BY DATE(u.createdAt)")
    List<Map<String, Object>> getUserGrowthTrend(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    Long countByStatus(UserStatus status);
}
