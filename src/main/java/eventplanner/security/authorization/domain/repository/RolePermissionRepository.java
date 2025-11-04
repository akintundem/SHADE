package eventplanner.security.authorization.domain.repository;

import eventplanner.security.authorization.domain.entity.RolePermission;
import eventplanner.common.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    
    List<RolePermission> findByRoleName(RoleName roleName);
    
    List<RolePermission> findByRoleNameAndContext(RoleName roleName, String context);
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleName = :roleName AND rp.permission.name = :permissionName")
    List<RolePermission> findByRoleNameAndPermissionName(@Param("roleName") RoleName roleName, @Param("permissionName") String permissionName);
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleName = :roleName AND rp.permission.name = :permissionName AND rp.context = :context")
    Optional<RolePermission> findByRoleNameAndPermissionNameAndContext(@Param("roleName") RoleName roleName, @Param("permissionName") String permissionName, @Param("context") String context);
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.permission.name = :permissionName")
    List<RolePermission> findByPermissionName(@Param("permissionName") String permissionName);
}
