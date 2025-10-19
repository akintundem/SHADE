package com.eventplanner.service;

import com.eventplanner.entity.Event;
import com.eventplanner.entity.Event.EventStatus;
import com.eventplanner.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    
    public Optional<Event> getEventById(UUID id) {
        return eventRepository.findById(id);
    }
    
    public Event createEvent(Event event) {
        event.setCreatedAt(java.time.LocalDateTime.now());
        event.setUpdatedAt(java.time.LocalDateTime.now());
        return eventRepository.save(event);
    }
    
    public Event updateEvent(UUID id, Event eventDetails) {
        Optional<Event> optionalEvent = eventRepository.findById(id);
        if (optionalEvent.isPresent()) {
            Event event = optionalEvent.get();
            event.setName(eventDetails.getName());
            event.setDescription(eventDetails.getDescription());
            event.setStartDate(eventDetails.getStartDate());
            event.setEndDate(eventDetails.getEndDate());
            event.setLocation(eventDetails.getLocation());
            event.setStatus(eventDetails.getStatus());
            event.setUpdatedAt(java.time.LocalDateTime.now());
            return eventRepository.save(event);
        }
        return null;
    }
    
    public boolean deleteEvent(UUID id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public List<Event> searchEventsByName(String name) {
        return eventRepository.findByNameContainingIgnoreCase(name);
    }
    
    public List<Event> getEventsByStatus(String status) {
        try {
            EventStatus eventStatus = EventStatus.valueOf(status.toUpperCase());
            return eventRepository.findByStatus(eventStatus);
        } catch (IllegalArgumentException e) {
            return List.of(); // Return empty list for invalid status
        }
    }
    
    public List<Event> searchEventsByLocation(String location) {
        return eventRepository.findByLocationContainingIgnoreCase(location);
    }
}
