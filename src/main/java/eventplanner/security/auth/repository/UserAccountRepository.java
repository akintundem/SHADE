package eventplanner.security.auth.repository;

import eventplanner.security.auth.enums.UserStatus;
import eventplanner.security.auth.enums.VisibilityLevel;
import eventplanner.security.auth.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByAuthSub(String authSub);
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    Page<UserAccount> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
    @Query("SELECT u FROM UserAccount u LEFT JOIN u.settings s " +
            "WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%')) " +
            "OR LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))) " +
            "AND (s.searchVisibility IS NULL OR s.searchVisibility = true) " +
            "AND (s.profileVisibility IS NULL OR s.profileVisibility <> :privateVisibility) " +
            "AND (u.status IS NULL OR u.status = :activeStatus)")
    Page<UserAccount> searchDirectoryUsers(@Param("term") String term,
                                           @Param("privateVisibility") VisibilityLevel privateVisibility,
                                           @Param("activeStatus") UserStatus activeStatus,
                                           Pageable pageable);

    @Query("SELECT u FROM UserAccount u LEFT JOIN u.settings s " +
            "WHERE (s.searchVisibility IS NULL OR s.searchVisibility = true) " +
            "AND (s.profileVisibility IS NULL OR s.profileVisibility <> :privateVisibility) " +
            "AND (u.status IS NULL OR u.status = :activeStatus)")
    Page<UserAccount> listDirectoryUsers(@Param("privateVisibility") VisibilityLevel privateVisibility,
                                         @Param("activeStatus") UserStatus activeStatus,
                                         Pageable pageable);
    Long countByStatus(UserStatus status);
}
