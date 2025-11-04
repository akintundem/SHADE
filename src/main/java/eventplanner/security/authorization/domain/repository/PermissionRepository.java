package eventplanner.security.authorization.domain.repository;

import eventplanner.security.authorization.domain.entity.Permission;
import eventplanner.common.domain.enums.ActionType;
import eventplanner.common.domain.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, java.util.UUID> {
    
    Optional<Permission> findByName(String name);
    
    List<Permission> findByResource(ResourceType resource);
    
    List<Permission> findByAction(ActionType action);
    
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action")
    Optional<Permission> findByResourceAndAction(@Param("resource") ResourceType resource, @Param("action") ActionType action);
    
    @Query("SELECT p FROM Permission p WHERE p.isSystemPermission = true")
    List<Permission> findSystemPermissions();
    
    @Query("SELECT p FROM Permission p WHERE p.isOrganizationPermission = true")
    List<Permission> findOrganizationPermissions();
    
    @Query("SELECT p FROM Permission p WHERE p.isEventPermission = true")
    List<Permission> findEventPermissions();
    
    List<Permission> findByNameIn(List<String> names);
}
