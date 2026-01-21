package eventplanner.features.social.repository;

import eventplanner.features.social.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {

    /**
     * Find follow relationship between two users
     */
    @Query("SELECT f FROM UserFollow f WHERE f.follower.id = :followerId AND f.followee.id = :followeeId")
    Optional<UserFollow> findByFollowerIdAndFolloweeId(
            @Param("followerId") UUID followerId,
            @Param("followeeId") UUID followeeId
    );

    /**
     * Check if a follow relationship exists
     */
    @Query("SELECT COUNT(f) > 0 FROM UserFollow f WHERE f.follower.id = :followerId AND f.followee.id = :followeeId AND f.status = 'ACTIVE'")
    boolean existsActiveFollowByFollowerIdAndFolloweeId(
            @Param("followerId") UUID followerId,
            @Param("followeeId") UUID followeeId
    );

    /**
     * Get all users that a user is following (active follows only)
     */
    @Query("SELECT f FROM UserFollow f WHERE f.follower.id = :userId AND f.status = 'ACTIVE' ORDER BY f.createdAt DESC")
    Page<UserFollow> findActiveFollowingByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * Get all followers of a user (active follows only)
     */
    @Query("SELECT f FROM UserFollow f WHERE f.followee.id = :userId AND f.status = 'ACTIVE' ORDER BY f.createdAt DESC")
    Page<UserFollow> findActiveFollowersByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * Count users that a user is following
     */
    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.follower.id = :userId AND f.status = 'ACTIVE'")
    long countActiveFollowingByUserId(@Param("userId") UUID userId);

    /**
     * Count followers of a user
     */
    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.followee.id = :userId AND f.status = 'ACTIVE'")
    long countActiveFollowersByUserId(@Param("userId") UUID userId);

    /**
     * Check if two users mutually follow each other
     */
    @Query("SELECT COUNT(f) = 2 FROM UserFollow f WHERE " +
           "((f.follower.id = :userId1 AND f.followee.id = :userId2) OR " +
           "(f.follower.id = :userId2 AND f.followee.id = :userId1)) AND " +
           "f.status = 'ACTIVE'")
    boolean areMutualFollowers(
            @Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2
    );

    /**
     * Delete follow relationship
     */
    @Query("DELETE FROM UserFollow f WHERE f.follower.id = :followerId AND f.followee.id = :followeeId")
    void deleteByFollowerIdAndFolloweeId(
            @Param("followerId") UUID followerId,
            @Param("followeeId") UUID followeeId
    );
}
