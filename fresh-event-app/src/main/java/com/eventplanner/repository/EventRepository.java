package com.eventplanner.repository;

import com.eventplanner.entity.Event;
import com.eventplanner.entity.Event.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    List<Event> findByNameContainingIgnoreCase(String name);
    
    List<Event> findByStatus(EventStatus status);
    
    List<Event> findByLocationContainingIgnoreCase(String location);
}
