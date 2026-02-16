package eventplanner.features.timeline.repository;

import eventplanner.features.timeline.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistRepository extends JpaRepository<Checklist, UUID> {
    
    @Query("SELECT c FROM Checklist c WHERE c.task.id = :taskId ORDER BY c.taskOrder ASC")
    List<Checklist> findByTaskIdOrderByTaskOrderAsc(@Param("taskId") UUID taskId);

    /**
     * Batch load checklists for multiple tasks in a single query to avoid N+1.
     */
    @Query("SELECT c FROM Checklist c WHERE c.task.id IN :taskIds ORDER BY c.task.id ASC, c.taskOrder ASC")
    List<Checklist> findByTaskIdInOrderByTaskOrderAsc(@Param("taskIds") List<UUID> taskIds);
}

