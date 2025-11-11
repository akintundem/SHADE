package eventplanner.features.timeline.service;

import eventplanner.common.domain.enums.TimelineChangeType;
import eventplanner.common.domain.enums.TimelineStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.timeline.dto.request.CreateTimelineItemRequest;
import eventplanner.features.timeline.dto.request.TimelineDependencyBatchRequest;
import eventplanner.features.timeline.dto.request.TimelinePublishRequest;
import eventplanner.features.timeline.dto.request.TimelineReorderRequest;
import eventplanner.features.timeline.dto.request.UpdateTimelineItemRequest;
import eventplanner.features.timeline.dto.response.TimelineItemResponse;
import eventplanner.features.timeline.entity.TimelineItem;
import eventplanner.features.timeline.repository.TimelineItemRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {

    private final TimelineItemRepository timelineItemRepository;
    private final EventRepository eventRepository;
    private final TimelineHistoryService timelineHistoryService;

    @Transactional(readOnly = true)
    public List<TimelineItemResponse> list(UUID eventId) {
        ensureEventExists(eventId);
        return timelineItemRepository.findByEventIdOrderByScheduledAtAsc(eventId)
                .stream()
                .sorted(Comparator.comparing(TimelineItem::getTaskOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TimelineItem::getScheduledAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TimelineItemResponse create(CreateTimelineItemRequest request, UserPrincipal user) {
        Event event = ensureEventExists(request.getEventId());

        TimelineItem item = new TimelineItem();
        item.setEventId(request.getEventId());
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());

        LocalDateTime scheduledAt = Optional.ofNullable(request.getScheduledAt())
                .orElse(request.getStartDate());
        item.setScheduledAt(scheduledAt);
        item.setStartDate(Optional.ofNullable(request.getStartDate()).orElse(scheduledAt));
        item.setEndTime(request.getEndTime());

        if (request.getDurationMinutes() != null && item.getStartDate() != null) {
            item.setDurationMinutes(request.getDurationMinutes());
            item.setEndTime(item.getStartDate().plusMinutes(request.getDurationMinutes()));
        } else {
            item.setDurationMinutes(request.getDurationMinutes());
        }
        item.setDueDate(item.getEndTime());

        item.setAssignedTo(request.getAssignedTo());
        item.setCategory(request.getCategory());
        item.setTaskOrder(Optional.ofNullable(request.getTaskOrder())
                .orElse(determineNextTaskOrder(request.getEventId())));

        TimelineStatus status = Optional.ofNullable(request.getStatus()).orElse(TimelineStatus.PENDING);
        item.setStatus(status);

        if (request.getDependencies() != null && request.getDependencies().length > 0) {
            List<UUID> dependencyList = Arrays.stream(request.getDependencies()).filter(Objects::nonNull).toList();
            validateDependencies(request.getEventId(), dependencyList, null);
            item.setDependencies(dependencyList.toArray(UUID[]::new));
        }

        TimelineItem saved = timelineItemRepository.save(item);
        log.debug("Created timeline item {} for event {}", saved.getId(), request.getEventId());

        timelineHistoryService.recordChange(
                event.getId(),
                saved.getId(),
                TimelineChangeType.CREATED,
                saved.getTitle(),
                "Timeline item created",
                user,
                Map.of(
                        "taskOrder", saved.getTaskOrder(),
                        "scheduledAt", saved.getScheduledAt()
                )
        );

        return toResponse(saved);
    }

    @Transactional
    public TimelineItemResponse update(UUID eventId,
                                       UUID itemId,
                                       UpdateTimelineItemRequest request,
                                       UserPrincipal user) {
        ensureEventExists(eventId);
        TimelineItem item = ensureItem(eventId, itemId);

        applyUpdate(item, request, eventId);

        TimelineItem saved = timelineItemRepository.save(item);
        log.debug("Updated timeline item {} for event {}", saved.getId(), eventId);

        timelineHistoryService.recordChange(
                eventId,
                saved.getId(),
                TimelineChangeType.UPDATED,
                saved.getTitle(),
                "Timeline item updated",
                user,
                Map.of(
                        "status", Optional.ofNullable(saved.getStatus()).map(Enum::name).orElse(null),
                        "taskOrder", saved.getTaskOrder(),
                        "scheduledAt", saved.getScheduledAt()
                )
        );

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID eventId, UUID itemId, UserPrincipal user) {
        ensureEventExists(eventId);
        TimelineItem item = ensureItem(eventId, itemId);

        timelineItemRepository.delete(item);
        log.debug("Deleted timeline item {} for event {}", itemId, eventId);

        timelineHistoryService.recordChange(
                eventId,
                itemId,
                TimelineChangeType.DELETED,
                item.getTitle(),
                "Timeline item deleted",
                user,
                Collections.emptyMap()
        );
    }

    @Transactional
    public List<TimelineItemResponse> reorder(UUID eventId,
                                              TimelineReorderRequest request,
                                              UserPrincipal user) {
        ensureEventExists(eventId);

        Map<UUID, TimelineReorderRequest.ItemOrderUpdate> updates = request.getItems()
                .stream()
                .collect(Collectors.toMap(TimelineReorderRequest.ItemOrderUpdate::getItemId, Function.identity()));

        List<TimelineItem> items = timelineItemRepository.findAllById(updates.keySet());
        if (items.size() != updates.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more timeline items do not exist");
        }

        for (TimelineItem item : items) {
            if (!item.getEventId().equals(eventId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timeline item does not belong to event");
            }
            TimelineReorderRequest.ItemOrderUpdate update = updates.get(item.getId());

            if (update.getTaskOrder() != null) {
                item.setTaskOrder(update.getTaskOrder());
            }
            if (update.getScheduledAt() != null) {
                item.setScheduledAt(update.getScheduledAt());
                item.setStartDate(update.getScheduledAt());
            }
        }

        timelineItemRepository.saveAll(items);
        timelineHistoryService.recordChange(
                eventId,
                null,
                TimelineChangeType.REORDERED,
                "Timeline reordered",
                "Timeline reorder operation affecting %d items".formatted(items.size()),
                user,
                Map.of("count", items.size())
        );

        return items.stream()
                .sorted(Comparator.comparing(TimelineItem::getTaskOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(TimelineItem::getScheduledAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TimelineItemResponse> updateDependencies(UUID eventId,
                                                         TimelineDependencyBatchRequest request,
                                                         UserPrincipal user) {
        ensureEventExists(eventId);

        Map<UUID, TimelineDependencyBatchRequest.DependencyUpdate> updates = request.getUpdates()
                .stream()
                .collect(Collectors.toMap(TimelineDependencyBatchRequest.DependencyUpdate::getItemId, Function.identity()));

        List<TimelineItem> items = timelineItemRepository.findAllById(updates.keySet());
        if (items.size() != updates.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more timeline items do not exist");
        }

        for (TimelineItem item : items) {
            if (!item.getEventId().equals(eventId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timeline item does not belong to event");
            }
            TimelineDependencyBatchRequest.DependencyUpdate update = updates.get(item.getId());
            List<UUID> dependencies = Optional.ofNullable(update.getDependencies())
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();

            validateDependencies(eventId, dependencies, item.getId());
            item.setDependencies(dependencies.isEmpty() ? null : dependencies.toArray(UUID[]::new));
        }

        timelineItemRepository.saveAll(items);

        timelineHistoryService.recordChange(
                eventId,
                null,
                TimelineChangeType.DEPENDENCIES_UPDATED,
                "Timeline dependencies updated",
                "Batch dependency update affecting %d items".formatted(items.size()),
                user,
                Map.of("count", items.size())
        );

        return items.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Event publishTimeline(UUID eventId,
                                 TimelinePublishRequest request,
                                 UserPrincipal user) {
        Event event = ensureEventExists(eventId);

        boolean publish = Boolean.TRUE.equals(request.getPublished());
        event.setTimelinePublished(publish);
        event.setTimelinePublishMessage(request.getMessage());
        if (publish) {
            event.setTimelinePublishedAt(LocalDateTime.now());
            event.setTimelinePublishedBy(user != null ? user.getId() : null);
        } else {
            event.setTimelinePublishedAt(null);
            event.setTimelinePublishedBy(null);
        }

        Event saved = eventRepository.save(event);

        timelineHistoryService.recordChange(
                eventId,
                null,
                publish ? TimelineChangeType.PUBLISHED : TimelineChangeType.UNPUBLISHED,
                publish ? "Timeline published" : "Timeline unpublished",
                Optional.ofNullable(request.getMessage()).orElse(""),
                user,
                Map.of(
                        "published", publish,
                        "actorId", user != null ? user.getId() : null
                )
        );

        return saved;
    }

    private Event ensureEventExists(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private TimelineItem ensureItem(UUID eventId, UUID itemId) {
        return timelineItemRepository.findById(itemId)
                .filter(item -> item.getEventId().equals(eventId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeline item not found"));
    }

    private void applyUpdate(TimelineItem item,
                             UpdateTimelineItemRequest request,
                             UUID eventId) {
        if (request.getTitle() != null) {
            item.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getScheduledAt() != null) {
            item.setScheduledAt(request.getScheduledAt());
        }
        if (request.getStartDate() != null) {
            item.setStartDate(request.getStartDate());
        }
        if (request.getEndTime() != null) {
            item.setEndTime(request.getEndTime());
            item.setDueDate(request.getEndTime());
        }
        if (request.getDurationMinutes() != null) {
            item.setDurationMinutes(request.getDurationMinutes());
            if (item.getStartDate() != null) {
                item.setEndTime(item.getStartDate().plusMinutes(request.getDurationMinutes()));
                item.setDueDate(item.getEndTime());
            }
        }
        if (request.getAssignedTo() != null) {
            item.setAssignedTo(request.getAssignedTo());
        }
        if (request.getCategory() != null) {
            item.setCategory(request.getCategory());
        }
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }
        if (request.getTaskOrder() != null) {
            item.setTaskOrder(request.getTaskOrder());
        }
        if (request.getDependencies() != null) {
            List<UUID> dependencies = Arrays.stream(request.getDependencies())
                    .filter(Objects::nonNull)
                    .toList();
            validateDependencies(eventId, dependencies, item.getId());
            item.setDependencies(dependencies.isEmpty() ? null : dependencies.toArray(UUID[]::new));
        }
    }

    private void validateDependencies(UUID eventId, List<UUID> dependencies, UUID currentItemId) {
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        Set<UUID> seen = dependencies.stream().collect(Collectors.toSet());
        if (seen.size() != dependencies.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate dependencies are not allowed");
        }

        if (currentItemId != null && dependencies.contains(currentItemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item cannot depend on itself");
        }

        List<TimelineItem> referencedItems = timelineItemRepository.findAllById(dependencies);
        if (referencedItems.size() != dependencies.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more dependency items do not exist");
        }

        boolean allSameEvent = referencedItems.stream()
                .allMatch(item -> item.getEventId().equals(eventId));

        if (!allSameEvent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dependencies must belong to the same event");
        }
    }

    private int determineNextTaskOrder(UUID eventId) {
        return timelineItemRepository.findByEventId(eventId)
                .stream()
                .map(TimelineItem::getTaskOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(0);
    }

    private TimelineItemResponse toResponse(TimelineItem item) {
        List<UUID> dependencies = item.getDependencies() != null
                ? Arrays.stream(item.getDependencies()).filter(Objects::nonNull).toList()
                : Collections.emptyList();

        return TimelineItemResponse.builder()
                .id(item.getId())
                .eventId(item.getEventId())
                .title(item.getTitle())
                .description(item.getDescription())
                .scheduledAt(item.getScheduledAt())
                .startDate(item.getStartDate())
                .endTime(item.getEndTime())
                .durationMinutes(item.getDurationMinutes())
                .assignedTo(item.getAssignedTo())
                .dependencies(dependencies)
                .status(item.getStatus())
                .category(item.getCategory())
                .taskOrder(item.getTaskOrder())
                .build();
    }
}
