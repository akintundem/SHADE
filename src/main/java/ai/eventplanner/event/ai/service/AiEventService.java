package ai.eventplanner.event.ai.service;

import ai.eventplanner.common.exception.ResourceNotFoundException;
import ai.eventplanner.event.ai.dto.AiEventFlyerResponse;
import ai.eventplanner.event.ai.dto.AiEventPlanResponse;
import ai.eventplanner.event.ai.dto.AiEventRequest;
import ai.eventplanner.event.ai.dto.AiEventResponse;
import ai.eventplanner.event.ai.dto.AiEventTypeCapabilitiesResponse;
import ai.eventplanner.event.ai.dto.AiEventTypeResponse;
import ai.eventplanner.event.ai.dto.AiFlyerRequest;
import ai.eventplanner.event.ai.dto.AiPlanRequest;
import ai.eventplanner.event.ai.dto.AiWorkflowRequest;
import ai.eventplanner.event.ai.dto.AiWorkflowResponse;
import ai.eventplanner.event.ai.model.AiEventRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class AiEventService {

    private final ConcurrentMap<UUID, AiEventRecord> events = new ConcurrentHashMap<>();

    public AiEventResponse createEvent(AiEventRequest request, UUID defaultOrganizerId) {
        UUID organizer = extractOrganizerId(request.getOrganizerId(), defaultOrganizerId);
        OffsetDateTime eventDate = parseDate(request.getDate(), OffsetDateTime.now().plusDays(60));
        UUID eventId = UUID.randomUUID();
        AiEventRecord record = AiEventRecord.builder()
                .id(eventId)
                .organizerId(organizer)
                .name(request.getName())
                .type(request.getType())
                .date(eventDate)
                .location(request.getLocation())
                .guestCount(request.getGuestCount())
                .budget(request.getBudget())
                .description(request.getDescription())
                .preferences(request.getPreferences() == null ? List.of() : new ArrayList<>(request.getPreferences()))
                .notes(request.getNotes())
                .status("planning")
                .aiGenerated(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        events.put(eventId, record);
        return toResponse(record);
    }

    public AiEventResponse getEvent(UUID eventId) {
        AiEventRecord record = events.get(eventId);
        if (record == null) {
            throw new ResourceNotFoundException("AI event not found");
        }
        return toResponse(record);
    }

    public void deleteEvent(UUID eventId) {
        events.remove(eventId);
    }

    public List<AiEventTypeResponse> getEventTypes() {
        return List.of(
                AiEventTypeResponse.builder().id("CONFERENCE").name("Conference").description("Professional gatherings focused on knowledge sharing and networking").build(),
                AiEventTypeResponse.builder().id("WEDDING").name("Wedding").description("Celebrations of love with curated experiences for guests").build(),
                AiEventTypeResponse.builder().id("FESTIVAL").name("Festival").description("Large scale festivals with multiple experiences and performances").build(),
                AiEventTypeResponse.builder().id("CORPORATE_RETREAT").name("Corporate Retreat").description("Immersive offsites for team alignment and culture building").build()
        );
    }

    public AiEventTypeCapabilitiesResponse getEventTypeCapabilities(String eventType) {
        String upper = eventType.toUpperCase();
        switch (upper) {
            case "CONFERENCE":
                return AiEventTypeCapabilitiesResponse.builder()
                        .eventType("CONFERENCE")
                        .eventTypeName("Conference")
                        .description("End-to-end planning support for professional conferences")
                        .keyFeatures(List.of(
                                "Agenda design and speaker coordination",
                                "Sponsor and exhibitor management",
                                "Attendee engagement workflows",
                                "Hybrid/virtual streaming setup guidance"
                        ))
                        .budgetCategories(List.of(
                                "Venue",
                                "Production",
                                "Technology",
                                "Catering",
                                "Marketing",
                                "Hospitality"
                        ))
                        .recommendedVendors(List.of("AV Production", "Catering", "Registration Platform", "Security"))
                        .technologyNeeds(List.of("High-density WiFi", "LED walls", "Simultaneous translation"))
                        .timelineMonths(List.of(
                                Map.of("title", "Speaker outreach", "daysBefore", 90),
                                Map.of("title", "Sponsor confirmation", "daysBefore", 75),
                                Map.of("title", "Final agenda", "daysBefore", 30)
                        ))
                        .build();
            case "WEDDING":
                return AiEventTypeCapabilitiesResponse.builder()
                        .eventType("WEDDING")
                        .eventTypeName("Wedding")
                        .description("Comprehensive planning for modern and traditional weddings")
                        .keyFeatures(List.of(
                                "Vendor curation and negotiation",
                                "Guest experience personalization",
                                "Ceremony and reception design",
                                "Cultural tradition integration"
                        ))
                        .budgetCategories(List.of(
                                "Venue",
                                "Catering",
                                "Decor",
                                "Photography",
                                "Entertainment",
                                "Attire"
                        ))
                        .recommendedVendors(List.of("Florist", "Photographer", "Live Entertainment", "Cake Designer"))
                        .technologyNeeds(List.of("Lighting design", "Sound reinforcement", "Live streaming"))
                        .timelineMonths(List.of(
                                Map.of("title", "Venue selection", "daysBefore", 180),
                                Map.of("title", "Invitations sent", "daysBefore", 120),
                                Map.of("title", "Rehearsal", "daysBefore", 2)
                        ))
                        .build();
            default:
                return AiEventTypeCapabilitiesResponse.builder()
                        .eventType(upper)
                        .eventTypeName(upper.charAt(0) + upper.substring(1).toLowerCase().replace('_', ' '))
                        .description("Custom event support with curated recommendations")
                        .keyFeatures(List.of("Budget optimization", "Timeline automation", "Vendor matching"))
                        .budgetCategories(List.of("Venue", "Catering", "Logistics", "Experiences"))
                        .recommendedVendors(List.of("Catering", "Production", "Logistics"))
                        .technologyNeeds(List.of("Projector", "Sound system"))
                        .timelineMonths(List.of(
                                Map.of("title", "Planning kickoff", "daysBefore", 90),
                                Map.of("title", "Vendor confirmations", "daysBefore", 45)
                        ))
                        .build();
        }
    }

    public Map<String, Object> getAiCapabilities() {
        return Map.of(
                "eventPlanning", true,
                "conversationalAI", true,
                "generativeAgenda", true,
                "vendorIntelligence", true,
                "flyerGeneration", true,
                "riskAnalysis", true,
                "budgetOptimization", true,
                "workflowOrchestration", Map.of(
                        "automatedSteps", List.of(
                                "plan_generation",
                                "vendor_matching",
                                "timeline_build",
                                "stakeholder_brief"
                        ),
                        "humanInLoop", true
                ),
                "supportedEventTypes", getEventTypes().stream().map(AiEventTypeResponse::getId).collect(Collectors.toList())
        );
    }

    public AiEventPlanResponse generatePlan(UUID eventId, AiPlanRequest request) {
        AiEventRecord record = events.get(eventId);
        if (record == null) {
            throw new ResourceNotFoundException("AI event not found");
        }
        OffsetDateTime eventDate = record.getDate();
        return AiEventPlanResponse.builder()
                .eventId(eventId)
                .eventName(request.getEventName())
                .eventType(request.getEventType())
                .date(request.getDate())
                .location(request.getLocation())
                .guestCount(request.getGuestCount())
                .budget(request.getBudget())
                .recommendations(List.of(
                        "Confirm venue technical requirements",
                        "Schedule keynote speaker dry-run",
                        "Publish attendee portal"
                ))
                .budgetBreakdown(Map.of(
                        "venue", request.getBudget().multiply(BigDecimal.valueOf(0.35)),
                        "catering", request.getBudget().multiply(BigDecimal.valueOf(0.3)),
                        "production", request.getBudget().multiply(BigDecimal.valueOf(0.2)),
                        "marketing", request.getBudget().multiply(BigDecimal.valueOf(0.1)),
                        "contingency", request.getBudget().multiply(BigDecimal.valueOf(0.05))
                ))
                .timeline(List.of(
                        Map.of("milestone", "Venue walkthrough", "date", eventDate.minusDays(45).toLocalDate().toString()),
                        Map.of("milestone", "Speaker confirmations", "date", eventDate.minusDays(30).toLocalDate().toString()),
                        Map.of("milestone", "Final production rehearsal", "date", eventDate.minusDays(2).toLocalDate().toString())
                ))
                .vendorRecommendations(List.of(
                        Map.of("category", "AV Production", "recommended", List.of("Visionary AV", "FutureSight Productions")),
                        Map.of("category", "Catering", "recommended", List.of("Epicurean Events", "GreenLeaf Catering"))
                ))
                .nextSteps(List.of("Send save-the-dates", "Publish sponsor prospectus", "Launch registration campaign"))
                .status("ready")
                .build();
    }

    public AiEventFlyerResponse generateFlyer(UUID eventId, AiFlyerRequest request) {
        if (!events.containsKey(eventId)) {
            throw new ResourceNotFoundException("AI event not found");
        }
        String baseUrl = "https://cdn.eventplanner.ai/assets/" + eventId;
        return AiEventFlyerResponse.builder()
                .eventId(eventId)
                .status("generated")
                .message("Flyer design generated successfully")
                .flyerUrl(baseUrl + "/flyer.pdf")
                .thumbnailUrl(baseUrl + "/thumbnail.png")
                .printUrl(baseUrl + "/print-ready.pdf")
                .designDescription(String.format("A %s style design highlighting %s with color palette %s", request.getStyle(), request.getTheme(), request.getColorScheme()))
                .build();
    }

    public AiWorkflowResponse executeWorkflow(AiWorkflowRequest request) {
        AiEventRecord record = events.get(request.getEventId());
        if (record == null) {
            throw new ResourceNotFoundException("AI event not found");
        }
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusSeconds(2);
        return AiWorkflowResponse.builder()
                .eventId(request.getEventId())
                .status("completed")
                .startedAt(start)
                .completedAt(end)
                .completedSteps(List.of(
                        "validated-event-brief",
                        "generated-initial-plan",
                        "assembled-vendor-shortlist",
                        "created-stakeholder-brief"
                ))
                .generatedArtifacts(Map.of(
                        "planningDocument", "https://cdn.eventplanner.ai/workflows/" + request.getEventId() + "/plan.docx",
                        "timeline", "https://cdn.eventplanner.ai/workflows/" + request.getEventId() + "/timeline.xlsx"
                ))
                .build();
    }

    public AiEventResponse updateStatus(UUID eventId, String status) {
        AiEventRecord record = events.get(eventId);
        if (record == null) {
            throw new ResourceNotFoundException("AI event not found");
        }
        record.setStatus(status);
        record.setUpdatedAt(OffsetDateTime.now());
        return toResponse(record);
    }

    private AiEventResponse toResponse(AiEventRecord record) {
        return AiEventResponse.builder()
                .id(record.getId())
                .name(record.getName())
                .type(record.getType())
                .date(record.getDate().toString())
                .location(record.getLocation())
                .organizerId(record.getOrganizerId())
                .guestCount(record.getGuestCount())
                .budget(record.getBudget())
                .description(record.getDescription())
                .preferences(record.getPreferences())
                .notes(record.getNotes())
                .status(record.getStatus())
                .aiGenerated(record.isAiGenerated())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private UUID extractOrganizerId(String organizerId, UUID fallback) {
        if (organizerId == null || organizerId.isBlank() || organizerId.equalsIgnoreCase("undefined") || organizerId.equalsIgnoreCase("null")) {
            return fallback;
        }
        try {
            return UUID.fromString(organizerId);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private OffsetDateTime parseDate(String value, OffsetDateTime defaultValue) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("undefined") || value.equalsIgnoreCase("null")) {
            return defaultValue;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(value);
                return ldt.atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ex2) {
                try {
                    LocalDate ld = LocalDate.parse(value);
                    return ld.atStartOfDay().atOffset(ZoneOffset.UTC);
                } catch (DateTimeParseException ex3) {
                    return defaultValue;
                }
            }
        }
    }
}
