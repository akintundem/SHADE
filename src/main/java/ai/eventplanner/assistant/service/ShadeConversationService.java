package ai.eventplanner.assistant.service;

import ai.eventplanner.assistant.client.OpenAiClient;
import ai.eventplanner.assistant.dto.ShadeConversationRequest;
import ai.eventplanner.assistant.dto.ShadeConversationResponse;
import ai.eventplanner.event.dto.request.CreateEventRequest;
import ai.eventplanner.assistant.entity.AssistantMessageEntity;
import ai.eventplanner.assistant.entity.AssistantSessionEntity;
import ai.eventplanner.assistant.repository.AssistantMessageRepository;
import ai.eventplanner.assistant.repository.AssistantSessionRepository;
import ai.eventplanner.event.service.EventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for Shade AI assistant conversations focused on event planning
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShadeConversationService {

    private static final String SHADE_SYSTEM_PROMPT = """
            You are **Shade**, a warm, professional, and intelligent event planner assistant.

            🎯 **Primary Purpose**
            You are responsible ONLY for **event planning and event creation conversations**.  
            You help users plan, define, and create events by collecting all required details and interacting with a backend service (via DTOs) to perform CRUD operations.

            If the user asks you anything unrelated to event planning or event management, respond politely with:
            > "I'm sorry, I can only help you with event planning and event creation tasks."

            ### 🧠 **Your Responsibilities**

            1. **Understand Intent**
               - Detect when a user wants to create, update, view, or delete an event.
               - Example intents:  
                 - "I want to plan a birthday party" → create event  
                 - "Change the date of my event" → update event  
                 - "Show me my events" → list events  

            2. **Collect Required Information**
               - Use the event DTO schema to determine what fields are required.
               - Ask the user questions conversationally until all required fields are collected.
               - Required fields: name, type, date, location, organizerId
               - Optional fields: guestCount, budget, description, preferences, notes, theme

            3. **Dynamic Field Handling**
               - For each missing field in the DTO, ask an intelligent question to collect it.
               - Example:  
                 - Missing "date" → "When is the event happening?"  
                 - Missing "location" → "Where would you like the event to take place?"  
                 - Missing "theme" → "What's the theme or style for your event?"  

            4. **Validation and Confirmation**
               - Once all required DTO fields are gathered, confirm with the user:
                 > "Perfect! Here's what I have so far: … Would you like me to create the event?"
               - Only after user confirmation, proceed to create the event via the backend service.

            5. **Interacting with Backend**
               - Use the DTO as the structured format for the request.
               - If creation succeeds → respond happily and show event summary.  
               - If creation fails → explain what went wrong and guide the user to fix it.

            6. **Politeness and Tone**
               - Always be professional, warm, and encouraging.  
               - Never rush the user.  
               - Respond naturally, as a real human planner would.

            ### 🧾 **Technical Behavior**

            - Use the DTO definition to guide which questions to ask.
            - If you have all the DTO fields, respond with a JSON object named `createEvent` like:
              ```json
              {
                "action": "createEvent",
                "arguments": {
                  "name": "Barbie Party",
                  "type": "BIRTHDAY_PARTY",
                  "date": "2025-12-10T18:00:00",
                  "location": "Downtown Lounge",
                  "organizerId": "123e4567-e89b-12d3-a456-426614174000",
                  "guestCount": 40,
                  "theme": "Barbie"
                }
              }
            ```

            * If you're missing any DTO field, respond naturally and **ask for it**.
            * If the user changes their mind, update your internal state accordingly.

            ### 🛡️ **Behavior Rules**

            * Never handle topics outside event planning.
            * Never output raw code to the user.
            * Never make assumptions about unknown details; instead, ask for clarification.
            * Always ensure the user explicitly confirms before any action (like creation or deletion).
            * Always explain clearly what you're doing.

            ### 🧩 **Example Flow**

            **User:**
            I want to create a beach wedding.

            **Shade:**
            That sounds wonderful! How many guests are you planning to invite?

            **User:**
            About 100.

            **Shade:**
            Got it. When is the wedding taking place?

            **User:**
            June 20th next year.

            **Shade:**
            Perfect! Where would you like the wedding to be held?

            **User:**
            At Coral Bay Resort.

            **Shade:**
            Here's what I have so far:

            * Event: Beach Wedding
            * Guests: 100
            * Date: June 20, 2026
            * Location: Coral Bay Resort

            Would you like me to go ahead and create the event?

            **User:**
            Yes, please.

            **Shade:**
            Creating your event now… ✅ Successfully created!
            Your "Beach Wedding" has been saved in our system. 🎉

            ### 🧠 **Fallback Response Example**

            If user says:

            > "What's the capital of France?"

            Shade should respond:

            > "I'm sorry, I can only assist with event planning and event management. Would you like to create or view an event instead?"

            ### ⚙️ **Integration Notes (for developers)**

            * The orchestrator should maintain conversation state and pass partial DTO data between turns.
            * Shade should return structured JSON when ready to call backend functions.
            * All backend communication (event creation, updates) should be handled by your Spring Boot service.
            """;

    private static final Pattern JSON_ACTION_PATTERN = Pattern.compile("\\{[^}]*\"action\"[^}]*\\}", Pattern.DOTALL);
    private static final UUID FALLBACK_ORGANIZER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    private final AssistantSessionRepository sessionRepository;
    private final AssistantMessageRepository messageRepository;
    private final EventService eventService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public ShadeConversationResponse converse(ShadeConversationRequest request) {
        // Get or create session
        AssistantSessionEntity session = getOrCreateSession(request.getSessionId());
        session = applySessionUpdates(session, request);
        
        // Save user message
        messageRepository.save(AssistantMessageEntity.userMessage(session.getId(), request.getMessage()));
        
        // Get conversation history
        List<AssistantMessageEntity> history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        
        // Generate Shade's response
        ShadeConversationResponse response = generateShadeResponse(session, history, request);
        
        // Save Shade's response
        messageRepository.save(AssistantMessageEntity.assistantMessage(
                session.getId(),
                response.getMessage(),
                response.getFollowUpQuestions(),
                response.getSuggestions()
        ));
        
        return response;
    }

    private ShadeConversationResponse generateShadeResponse(AssistantSessionEntity session,
                                                            List<AssistantMessageEntity> history,
                                                            ShadeConversationRequest request) {
        if (openAiClient.isConfigured()) {
            return generateWithAI(session, history, request);
        }
        return buildLlmUnavailableResponse(session);
    }

    private ShadeConversationResponse generateWithAI(AssistantSessionEntity session,
                                                     List<AssistantMessageEntity> history,
                                                     ShadeConversationRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(message("system", SHADE_SYSTEM_PROMPT));
        messages.add(message("system", buildSessionContext(session)));

        history.forEach(message -> {
            String role = "assistant".equalsIgnoreCase(message.getRole()) ? "assistant" : "user";
            messages.add(message(role, message.getContent()));
        });

        messages.add(message("user", request.getMessage()));

        try {
            String response = openAiClient.createChatCompletion(messages);
            return parseShadeResponse(response, session, request);
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            return buildLlmUnavailableResponse(session);
        }
    }

    private ShadeConversationResponse parseShadeResponse(String response, 
                                                        AssistantSessionEntity session, 
                                                        ShadeConversationRequest request) {
        // Check if response contains a JSON action
        Matcher matcher = JSON_ACTION_PATTERN.matcher(response);
        if (matcher.find()) {
            try {
                String jsonAction = matcher.group();
                @SuppressWarnings("unchecked")
                Map<String, Object> actionData = objectMapper.readValue(jsonAction, Map.class);
                
                if ("createEvent".equals(actionData.get("action"))) {
                    return handleCreateEventAction(actionData, session, response);
                }
            } catch (JsonProcessingException e) {
                log.debug("Failed to parse JSON action", e);
            }
        }
        
        // Regular conversational response
        return ShadeConversationResponse.builder()
                .sessionId(session.getId())
                .message(response)
                .intent(detectIntent(request.getMessage()))
                .collectedData(extractEventData(request.getMessage(), session))
                .missingFields(getMissingRequiredFields(session))
                .followUpQuestions(generateFollowUpQuestions(session))
                .suggestions(generateSuggestions(session))
                .action(ShadeConversationResponse.ShadeAction.builder()
                        .type("none")
                        .ready(false)
                        .build())
                .timestamp(OffsetDateTime.now())
                .requiresConfirmation(false)
                .build();
    }

    private ShadeConversationResponse handleCreateEventAction(Map<String, Object> actionData, 
                                                             AssistantSessionEntity session, 
                                                             String originalResponse) {
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) actionData.get("arguments");
        
        if (arguments != null && hasAllRequiredFields(arguments)) {
            return ShadeConversationResponse.builder()
                    .sessionId(session.getId())
                    .message(originalResponse)
                    .intent("createEvent")
                    .collectedData(arguments)
                    .missingFields(Collections.emptyList())
                    .followUpQuestions(Collections.emptyList())
                    .suggestions(Collections.emptyMap())
                    .action(ShadeConversationResponse.ShadeAction.builder()
                            .type("createEvent")
                            .arguments(arguments)
                            .ready(true)
                            .build())
                    .timestamp(OffsetDateTime.now())
                    .requiresConfirmation(true)
                    .confirmationMessage("Would you like me to create this event?")
                    .build();
        } else {
            return ShadeConversationResponse.builder()
                    .sessionId(session.getId())
                    .message(originalResponse)
                    .intent("createEvent")
                    .collectedData(arguments != null ? arguments : Collections.emptyMap())
                    .missingFields(getMissingFieldsFromArguments(arguments))
                    .followUpQuestions(generateFieldQuestions(arguments))
                    .suggestions(Collections.emptyMap())
                    .action(ShadeConversationResponse.ShadeAction.builder()
                            .type("createEvent")
                            .arguments(arguments)
                            .ready(false)
                            .build())
                    .timestamp(OffsetDateTime.now())
                    .requiresConfirmation(false)
                    .build();
        }
    }

    public ShadeConversationResponse createEvent(CreateEventRequest request) {
        try {
            // Convert CreateEventRequest to EventEntity using the same pattern as EventCrudController
            ai.eventplanner.event.model.EventEntity eventEntity = new ai.eventplanner.event.model.EventEntity();
            eventEntity.setOrganizerId(request.getOrganizerId());
            eventEntity.setName(request.getName());
            eventEntity.setType(request.getType());
            eventEntity.setDate(request.getDate());
            
            // Create metadata map with all the additional fields
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("location", request.getLocation());
            if (request.getGuestCount() != null) metadata.put("guestCount", request.getGuestCount());
            if (request.getBudget() != null) metadata.put("budget", request.getBudget());
            if (request.getDescription() != null) metadata.put("description", request.getDescription());
            if (request.getPreferences() != null) metadata.put("preferences", request.getPreferences());
            if (request.getNotes() != null) metadata.put("notes", request.getNotes());
            eventEntity.setMetadata(metadata);
            
            // Use the event service to create the event
            var createdEvent = eventService.create(eventEntity);
            
            return ShadeConversationResponse.builder()
                    .sessionId(UUID.randomUUID()) // You might want to track this differently
                    .message(String.format("🎉 Perfect! I've successfully created your \"%s\" event. " +
                            "Your event has been saved in our system and you can start planning the details.", 
                            request.getName()))
                    .intent("createEvent")
                    .collectedData(Map.of("eventId", createdEvent.getId().toString()))
                    .missingFields(Collections.emptyList())
                    .followUpQuestions(List.of(
                            "Would you like me to help you plan the timeline?",
                            "Should we start working on the guest list?",
                            "Would you like to explore vendor recommendations?"
                    ))
                    .suggestions(Map.of(
                            "nextSteps", List.of("Set up timeline", "Create guest list", "Find vendors", "Plan budget details"),
                            "eventTypes", List.of("CONFERENCE", "WEDDING", "BIRTHDAY_PARTY", "CORPORATE_EVENT")
                    ))
                    .action(ShadeConversationResponse.ShadeAction.builder()
                            .type("createEvent")
                            .arguments(Map.of("eventId", createdEvent.getId().toString()))
                            .ready(true)
                            .build())
                    .timestamp(OffsetDateTime.now())
                    .requiresConfirmation(false)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error creating event", e);
            return ShadeConversationResponse.builder()
                    .sessionId(UUID.randomUUID())
                    .message("I apologize, but I encountered an issue while creating your event. " +
                            "Please check the information you provided and try again. " +
                            "If the problem persists, please contact support.")
                    .intent("createEvent")
                    .collectedData(Collections.emptyMap())
                    .missingFields(Collections.emptyList())
                    .followUpQuestions(List.of("Would you like to try again?", "Should I help you review the event details?"))
                    .suggestions(Collections.emptyMap())
                    .action(ShadeConversationResponse.ShadeAction.builder()
                            .type("createEvent")
                            .ready(false)
                            .build())
                    .timestamp(OffsetDateTime.now())
                    .requiresConfirmation(false)
                    .build();
        }
    }

    // Helper methods
    private AssistantSessionEntity applySessionUpdates(AssistantSessionEntity session, ShadeConversationRequest request) {
        boolean updated = false;

        Map<String, Object> aggregatedData = new HashMap<>();
        if (request.getCollectedData() != null && !request.getCollectedData().isEmpty()) {
            aggregatedData.putAll(request.getCollectedData());
        }
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            aggregatedData.putAll(extractContextData(request.getContext()));
        }

        aggregatedData.putAll(extractStructuredDataFromMessage(request.getMessage(), session));

        updated |= applyAggregatedDataToSession(session, aggregatedData);
        updated |= applyContextPayload(session, request);

        if (session.getOrganizerId() == null) {
            session.setOrganizerId(resolveDefaultOrganizerId());
            updated = true;
        }

        updated |= updateSessionProgress(session);

        if (updated) {
            session = sessionRepository.save(session);
        }

        return session;
    }

    private AssistantSessionEntity getOrCreateSession(UUID sessionId) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                    .orElseGet(() -> {
                        // If session doesn't exist, create a new one
                        log.info("Session {} not found, creating new session", sessionId);
                        AssistantSessionEntity newSession = new AssistantSessionEntity();
                        newSession.setDomain("EVENT");
                        newSession.setStatus("in_discovery");
                        newSession.setAiGenerated(true);
                        return sessionRepository.save(newSession);
                    });
        } else {
            AssistantSessionEntity session = new AssistantSessionEntity();
            session.setDomain("EVENT");
            session.setStatus("in_discovery");
            session.setAiGenerated(true);
            return sessionRepository.save(session);
        }
    }

    private Map<String, Object> extractContextData(Map<String, Object> context) {
        Map<String, Object> extracted = new HashMap<>();
        context.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            String normalizedKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
            switch (normalizedKey) {
                case "organizerid":
                case "organizer_id":
                case "user_id":
                case "userId":
                    extracted.put("organizerId", value);
                    break;
                case "name":
                case "event_name":
                    extracted.put("name", value);
                    break;
                case "type":
                case "event_type":
                    extracted.put("type", value);
                    break;
                case "date":
                case "event_date":
                    extracted.put("date", value);
                    break;
                case "location":
                case "event_location":
                    extracted.put("location", value);
                    break;
                case "guestcount":
                case "guests":
                case "guest_count":
                    extracted.put("guestCount", value);
                    break;
                case "budget":
                    extracted.put("budget", value);
                    break;
                case "description":
                    extracted.put("description", value);
                    break;
                case "preferences":
                    extracted.put("preferences", value);
                    break;
                default:
                    // ignore
            }
        });
        return extracted;
    }

    private Map<String, Object> extractStructuredDataFromMessage(String message, AssistantSessionEntity session) {
        Map<String, Object> data = new HashMap<>();
        if (!StringUtils.hasText(message)) {
            return data;
        }

        Map<String, Object> extracted = new HashMap<>();
        extractFromMessage(message, extracted);
        data.putAll(extracted);

        parseEventName(message, session).ifPresent(name -> data.put("name", name));
        parseEventDate(message).ifPresent(date -> data.put("date", date));
        parseLocation(message).ifPresent(location -> data.put("location", location));

        return data;
    }

    private boolean applyAggregatedDataToSession(AssistantSessionEntity session, Map<String, Object> data) {
        boolean updated = false;

        if (data.containsKey("name")) {
            String name = cleanString(data.get("name"));
            if (StringUtils.hasText(name) && !name.equals(session.getName())) {
                session.setName(name);
                updated = true;
            }
        }

        if (data.containsKey("type")) {
            String type = cleanString(data.get("type"));
            if (StringUtils.hasText(type)) {
                String normalizedType = type.toUpperCase(Locale.ROOT).replace(' ', '_');
                if (!normalizedType.equals(session.getType())) {
                    session.setType(normalizedType);
                    updated = true;
                }
            }
        }

        if (data.containsKey("date")) {
            OffsetDateTime date = parseDateValue(data.get("date")).orElse(null);
            if (date != null && !date.equals(session.getDate())) {
                session.setDate(date);
                updated = true;
            }
        }

        if (data.containsKey("location")) {
            String location = cleanString(data.get("location"));
            if (StringUtils.hasText(location) && !location.equals(session.getLocation())) {
                session.setLocation(location);
                updated = true;
            }
        }

        if (data.containsKey("guestCount")) {
            Optional<Integer> guestCount = parseGuestCount(data.get("guestCount"));
            if (guestCount.isPresent() && !Objects.equals(session.getGuestCount(), guestCount.get())) {
                session.setGuestCount(guestCount.get());
                updated = true;
            }
        }

        if (data.containsKey("budget")) {
            Optional<BigDecimal> budget = parseBudget(data.get("budget"));
            if (budget.isPresent()) {
                BigDecimal normalizedBudget = budget.get();
                if (session.getBudget() == null || session.getBudget().compareTo(normalizedBudget) != 0) {
                    session.setBudget(normalizedBudget);
                    updated = true;
                }
            }
        }

        if (data.containsKey("description")) {
            String description = cleanString(data.get("description"));
            if (StringUtils.hasText(description) && !description.equals(session.getDescription())) {
                session.setDescription(description);
                updated = true;
            }
        }

        if (data.containsKey("preferences")) {
            List<String> preferences = normalizePreferences(data.get("preferences"));
            if (!preferences.isEmpty() && !preferences.equals(session.getPreferences())) {
                session.setPreferences(new ArrayList<>(preferences));
                updated = true;
            }
        }

        if (data.containsKey("organizerId")) {
            Optional<UUID> organizerId = parseUuid(data.get("organizerId"));
            if (organizerId.isPresent() && !organizerId.get().equals(session.getOrganizerId())) {
                session.setOrganizerId(organizerId.get());
                updated = true;
            }
        }

        return updated;
    }

    private boolean applyContextPayload(AssistantSessionEntity session, ShadeConversationRequest request) {
        if ((request.getContext() == null || request.getContext().isEmpty()) &&
            (request.getCollectedData() == null || request.getCollectedData().isEmpty())) {
            return false;
        }

        Map<String, Object> payload = new HashMap<>();
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            payload.put("context", request.getContext());
        }
        if (request.getCollectedData() != null && !request.getCollectedData().isEmpty()) {
            payload.put("collectedData", request.getCollectedData());
        }

        try {
            String serialized = objectMapper.writeValueAsString(payload);
            if (!serialized.equals(session.getContextPayload())) {
                session.setContextPayload(serialized);
                return true;
            }
        } catch (JsonProcessingException e) {
            log.debug("Failed to serialize assistant session context payload", e);
        }

        return false;
    }

    private UUID resolveDefaultOrganizerId() {
        String configured = environment.getProperty("assistant.default-organizer-id");
        if (StringUtils.hasText(configured)) {
            try {
                return UUID.fromString(configured.trim());
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid assistant.default-organizer-id value '{}'. Falling back to placeholder.", configured);
            }
        }
        return FALLBACK_ORGANIZER_ID;
    }

    private boolean updateSessionProgress(AssistantSessionEntity session) {
        List<String> missing = getMissingRequiredFields(session);
        String nextStatus;
        if (missing.isEmpty()) {
            nextStatus = "ready_to_confirm";
        } else if (missing.size() <= 2) {
            nextStatus = "collecting_remaining_details";
        } else {
            nextStatus = "in_discovery";
        }

        if (!Objects.equals(session.getStatus(), nextStatus)) {
            session.setStatus(nextStatus);
            return true;
        }
        return false;
    }

    private String buildSessionContext(AssistantSessionEntity session) {
        return String.format("Current session context: domain='%s', name='%s', type='%s', location='%s', guestCount=%s, budget=%s.",
                defaultString(session.getDomain()),
                defaultString(session.getName()),
                defaultString(session.getType()),
                defaultString(session.getLocation()),
                session.getGuestCount() == null ? "unknown" : session.getGuestCount(),
                session.getBudget() == null ? "unknown" : session.getBudget());
    }

    private String detectIntent(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Event creation intents
        if (lowerMessage.contains("create") || lowerMessage.contains("plan") || lowerMessage.contains("organize") || 
            lowerMessage.contains("birthday") || lowerMessage.contains("wedding") || lowerMessage.contains("party") ||
            lowerMessage.contains("conference") || lowerMessage.contains("event")) {
            return "createEvent";
        } 
        // Capabilities questions
        else if (lowerMessage.contains("what can you") || lowerMessage.contains("help me") || 
                 lowerMessage.contains("capabilities") || lowerMessage.contains("what do you do")) {
            return "capabilities";
        }
        // Event types questions
        else if (lowerMessage.contains("what types") || lowerMessage.contains("what kinds") || 
                 lowerMessage.contains("event types") || lowerMessage.contains("types of events")) {
            return "eventTypes";
        }
        // Specific event type questions
        else if (lowerMessage.contains("tell me about") || lowerMessage.contains("about birthday") ||
                 lowerMessage.contains("about wedding") || lowerMessage.contains("about conference")) {
            return "eventTypeDetails";
        }
        // Update/change intents
        else if (lowerMessage.contains("update") || lowerMessage.contains("change") || lowerMessage.contains("modify")) {
            return "updateEvent";
        }
        // List/view intents
        else if (lowerMessage.contains("list") || lowerMessage.contains("show") || lowerMessage.contains("view")) {
            return "listEvents";
        }
        // Delete intents
        else if (lowerMessage.contains("delete") || lowerMessage.contains("remove") || lowerMessage.contains("cancel")) {
            return "deleteEvent";
        }
        
        return "unknown";
    }


    private Map<String, Object> extractEventData(String message, AssistantSessionEntity session) {
        Map<String, Object> data = new HashMap<>();
        
        // Extract from session if available
        if (session.getName() != null) data.put("name", session.getName());
        if (session.getType() != null) data.put("type", session.getType());
        if (session.getDate() != null) data.put("date", session.getDate().toString());
        if (session.getLocation() != null) data.put("location", session.getLocation());
        if (session.getGuestCount() != null) data.put("guestCount", session.getGuestCount());
        if (session.getBudget() != null) data.put("budget", session.getBudget());
        if (session.getDescription() != null) data.put("description", session.getDescription());
        
        // Try to extract from message
        extractFromMessage(message, data);
        
        return data;
    }

    private void extractFromMessage(String message, Map<String, Object> data) {
        String lowerMessage = message.toLowerCase();
        
        // Extract numbers (guest count, budget)
        if (lowerMessage.contains("guest") || lowerMessage.contains("people") || lowerMessage.contains("attendee")) {
            Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
            Matcher matcher = numberPattern.matcher(message);
            if (matcher.find()) {
                data.put("guestCount", Integer.parseInt(matcher.group(1)));
            }
        }
        
        if (lowerMessage.contains("budget") || lowerMessage.contains("cost") || lowerMessage.contains("$")) {
            Pattern moneyPattern = Pattern.compile("\\$?(\\d+(?:\\.\\d{2})?)");
            Matcher matcher = moneyPattern.matcher(message);
            if (matcher.find()) {
                data.put("budget", Double.parseDouble(matcher.group(1)));
            }
        }
        
        // Extract event types
        if (lowerMessage.contains("birthday") || lowerMessage.contains("party")) {
            data.put("type", "BIRTHDAY_PARTY");
        } else if (lowerMessage.contains("wedding")) {
            data.put("type", "WEDDING");
        } else if (lowerMessage.contains("conference") || lowerMessage.contains("meeting")) {
            data.put("type", "CONFERENCE");
        } else if (lowerMessage.contains("corporate") || lowerMessage.contains("business")) {
            data.put("type", "CORPORATE_EVENT");
        }
    }

    private List<String> getMissingRequiredFields(AssistantSessionEntity session) {
        List<String> missing = new ArrayList<>();
        if (session.getName() == null) missing.add("name");
        if (session.getType() == null) missing.add("type");
        if (session.getDate() == null) missing.add("date");
        if (session.getLocation() == null) missing.add("location");
        if (session.getOrganizerId() == null) missing.add("organizerId");
        return missing;
    }

    private List<String> getMissingFieldsFromArguments(Map<String, Object> arguments) {
        if (arguments == null) return List.of("name", "type", "date", "location", "organizerId");
        
        List<String> missing = new ArrayList<>();
        if (!arguments.containsKey("name") || arguments.get("name") == null) missing.add("name");
        if (!arguments.containsKey("type") || arguments.get("type") == null) missing.add("type");
        if (!arguments.containsKey("date") || arguments.get("date") == null) missing.add("date");
        if (!arguments.containsKey("location") || arguments.get("location") == null) missing.add("location");
        if (!arguments.containsKey("organizerId") || arguments.get("organizerId") == null) missing.add("organizerId");
        return missing;
    }

    private boolean hasAllRequiredFields(Map<String, Object> arguments) {
        return arguments != null &&
               arguments.containsKey("name") && arguments.get("name") != null &&
               arguments.containsKey("type") && arguments.get("type") != null &&
               arguments.containsKey("date") && arguments.get("date") != null &&
               arguments.containsKey("location") && arguments.get("location") != null &&
               arguments.containsKey("organizerId") && arguments.get("organizerId") != null;
    }

    private String generateFieldQuestion(String field, Map<String, Object> collectedData) {
        switch (field) {
            case "name":
                return "What would you like to name your event?";
            case "type":
                return "What type of event are you planning? (e.g., birthday party, wedding, conference, corporate event)";
            case "date":
                return "When is the event taking place?";
            case "location":
                return "Where would you like the event to be held?";
            case "organizerId":
                return "I'll use your user ID as the organizer. Let me set that up for you.";
            case "guestCount":
                return "How many guests are you expecting?";
            case "budget":
                return "What's your budget for this event?";
            default:
                return "Could you provide more details about " + field + "?";
        }
    }

    private List<String> generateFieldQuestions(Map<String, Object> arguments) {
        List<String> missingFields = getMissingFieldsFromArguments(arguments);
        return missingFields.stream()
                .map(field -> generateFieldQuestion(field, arguments))
                .toList();
    }

    private List<String> generateFollowUpQuestions(AssistantSessionEntity session) {
        List<String> questions = new ArrayList<>();
        if (session.getGuestCount() == null) {
            questions.add("How many guests are you expecting?");
        }
        if (session.getBudget() == null) {
            questions.add("What's your budget for this event?");
        }
        if (session.getDescription() == null) {
            questions.add("Would you like to add a description for your event?");
        }
        return questions;
    }

    private Map<String, List<String>> generateSuggestions(AssistantSessionEntity session) {
        Map<String, List<String>> suggestions = new HashMap<>();
        suggestions.put("eventTypes", List.of("BIRTHDAY_PARTY", "WEDDING", "CONFERENCE", "CORPORATE_EVENT", "FESTIVAL"));
        suggestions.put("nextSteps", List.of("Set timeline", "Find vendors", "Create guest list", "Plan budget"));
        return suggestions;
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> map = new HashMap<>();
        map.put("role", role);
        map.put("content", content);
        return map;
    }

    private ShadeConversationResponse buildLlmUnavailableResponse(AssistantSessionEntity session) {
        List<String> missingFields = getMissingRequiredFields(session);
        return ShadeConversationResponse.builder()
                .sessionId(session.getId())
                .message("Having problem connecting to LLM. Please try again shortly.")
                .intent("systemIssue")
                .collectedData(extractEventData("", session))
                .missingFields(missingFields)
                .followUpQuestions(generateFollowUpQuestions(session))
                .suggestions(generateSuggestions(session))
                .action(ShadeConversationResponse.ShadeAction.builder()
                        .type("none")
                        .ready(false)
                        .build())
                .timestamp(OffsetDateTime.now())
                .requiresConfirmation(false)
                .build();
    }

    private String cleanString(Object value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.toString().trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        cleaned = cleaned.replaceAll("[\\u201C\\u201D\"]", "").trim();
        cleaned = cleaned.replaceAll("[\\.,!?]+$", "").trim();
        return cleaned;
    }

    private Optional<OffsetDateTime> parseDateValue(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof OffsetDateTime) {
            return Optional.of((OffsetDateTime) value);
        }
        if (value instanceof LocalDate) {
            return Optional.of(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        if (value instanceof String) {
            String stringValue = ((String) value).trim();
            if (!StringUtils.hasText(stringValue)) {
                return Optional.empty();
            }
            try {
                return Optional.of(OffsetDateTime.parse(stringValue));
            } catch (DateTimeParseException ignored) {
                // Fall through to conversational parsing
            }
            return parseEventDate(stringValue);
        }
        return Optional.empty();
    }

    private Optional<Integer> parseGuestCount(Object value) {
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }
        if (value instanceof String stringValue) {
            Matcher matcher = Pattern.compile("\\d+").matcher(stringValue);
            if (matcher.find()) {
                try {
                    return Optional.of(Integer.parseInt(matcher.group()));
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> parseBudget(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return Optional.of(bigDecimal);
        }
        if (value instanceof Number number) {
            return Optional.of(BigDecimal.valueOf(number.doubleValue()));
        }
        if (value instanceof String stringValue) {
            String cleaned = stringValue.replaceAll("[^0-9.]", "");
            if (StringUtils.hasText(cleaned)) {
                try {
                    return Optional.of(new BigDecimal(cleaned));
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
        }
        return Optional.empty();
    }

    private List<String> normalizePreferences(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(pref -> pref.replaceAll("[\\u201C\\u201D\"]", "").trim())
                    .toList();
        }
        if (value instanceof String stringValue) {
            if (!StringUtils.hasText(stringValue)) {
                return List.of();
            }
            String[] tokens = stringValue.split("[,;]");
            return Arrays.stream(tokens)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(token -> token.replaceAll("[\\u201C\\u201D\"]", "").trim())
                    .toList();
        }
        return List.of();
    }

    private Optional<UUID> parseUuid(Object value) {
        if (value instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                return Optional.of(UUID.fromString(stringValue.trim()));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid uuid
            }
        }
        return Optional.empty();
    }

    private Optional<String> parseEventName(String message, AssistantSessionEntity session) {
        if (!StringUtils.hasText(message)) {
            return Optional.empty();
        }

        Pattern explicitNamePattern = Pattern.compile("(?i)(?:name\\s*(?:is|=)?|call(?:\\s*it)?|it's called|its called|called)\\s*[:=-]?\\s*\"?([\\w\\s'&-]{3,})\"?");
        Matcher explicitMatcher = explicitNamePattern.matcher(message);
        if (explicitMatcher.find()) {
            String extracted = cleanString(explicitMatcher.group(1));
            if (StringUtils.hasText(extracted)) {
                return Optional.of(extracted);
            }
        }

        String trimmed = message.trim();
        if (session.getName() == null && !trimmed.endsWith("?")) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!(lower.contains("budget") || lower.contains("guest") || lower.contains("location") || lower.contains("date"))) {
                if (trimmed.length() <= 140) {
                    String candidate = cleanString(trimmed);
                    if (StringUtils.hasText(candidate)) {
                        return Optional.of(candidate);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private Optional<OffsetDateTime> parseEventDate(String message) {
        if (!StringUtils.hasText(message)) {
            return Optional.empty();
        }

        String normalized = message.replaceAll("(?i)(\\d+)(st|nd|rd|th)", "$1");
        List<String> candidates = new ArrayList<>();

        Matcher iso = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b").matcher(normalized);
        while (iso.find()) {
            candidates.add(iso.group());
        }

        Matcher slash = Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b").matcher(normalized);
        while (slash.find()) {
            candidates.add(slash.group());
        }

        Matcher monthFirst = Pattern.compile("\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\s+\\d{1,2}(?:,\\s*\\d{4})?\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        while (monthFirst.find()) {
            candidates.add(monthFirst.group());
        }

        Matcher dayFirst = Pattern.compile("\\b\\d{1,2}\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*(?:,\\s*\\d{4})?\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        while (dayFirst.find()) {
            candidates.add(dayFirst.group());
        }

        for (String candidate : candidates) {
            Optional<OffsetDateTime> parsed = tryParseDateCandidate(candidate);
            if (parsed.isPresent()) {
                return parsed;
            }
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("tomorrow")) {
            return Optional.of(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        if (lower.contains("day after tomorrow")) {
            return Optional.of(LocalDate.now().plusDays(2).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        if (lower.contains("next week")) {
            return Optional.of(LocalDate.now().plusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }
        if (lower.contains("next month")) {
            return Optional.of(LocalDate.now().plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
        }

        return Optional.empty();
    }

    private Optional<OffsetDateTime> tryParseDateCandidate(String candidate) {
        DateTimeFormatter twoDigitYearFormatter = DateTimeFormatter.ofPattern("M/d/yy");
        List<DateTimeFormatter> yearFormatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                twoDigitYearFormatter,
                DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)
        );

        String trimmed = candidate.trim();
        for (DateTimeFormatter formatter : yearFormatters) {
            try {
                LocalDate date = LocalDate.parse(trimmed, formatter);
                if (formatter == twoDigitYearFormatter) {
                    int year = date.getYear();
                    if (year < 100) {
                        int currentCentury = LocalDate.now().getYear() / 100 * 100;
                        date = date.withYear(currentCentury + year);
                    }
                }
                return Optional.of(date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }

        if (!trimmed.matches(".*\\d{4}.*")) {
            int currentYear = LocalDate.now().getYear();
            List<DateTimeFormatter> fallbackFormatters = List.of(
                    DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)
            );

            for (DateTimeFormatter formatter : fallbackFormatters) {
                try {
                    LocalDate date = LocalDate.parse(trimmed + " " + currentYear, formatter);
                    if (date.isBefore(LocalDate.now())) {
                        date = date.plusYears(1);
                    }
                    return Optional.of(date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime());
                } catch (DateTimeParseException ignored) {
                    // keep trying
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> parseLocation(String message) {
        if (!StringUtils.hasText(message)) {
            return Optional.empty();
        }
        Pattern locationPattern = Pattern.compile("(?i)\\b(?:at|in|located at|located in)\\s+([A-Za-z0-9][A-Za-z0-9\\s'&,.-]{2,})");
        Matcher matcher = locationPattern.matcher(message);
        if (matcher.find()) {
            String location = matcher.group(1).trim().replaceAll("[\\.,!?]+$", "").trim();
            if (StringUtils.hasText(location)) {
                return Optional.of(location);
            }
        }
        return Optional.empty();
    }


    private String defaultString(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    // New conversational query handlers
    
    private ShadeConversationResponse handleCapabilitiesQuery(AssistantSessionEntity session, ShadeConversationRequest request) {
        return ShadeConversationResponse.builder()
                .sessionId(session.getId())
                .message("I'm Shade, your intelligent event planning assistant! 🎉 " +
                        "I can help you plan and create amazing events including:\n\n" +
                        "• **Birthday Parties** - Fun celebrations with themes and entertainment\n" +
                        "• **Weddings** - Beautiful ceremonies with vendor coordination\n" +
                        "• **Conferences** - Professional gatherings with agenda planning\n" +
                        "• **Corporate Events** - Team building and business celebrations\n" +
                        "• **Festivals** - Large-scale events with multiple experiences\n" +
                        "• **Anniversaries** - Milestone celebrations\n" +
                        "• **Graduations** - Academic achievement celebrations\n" +
                        "• **Holiday Parties** - Seasonal gatherings\n\n" +
                        "I can help with venue selection, guest management, budget planning, timeline creation, and vendor recommendations. " +
                        "What type of event would you like to plan?")
                .intent("capabilities")
                .collectedData(Collections.emptyMap())
                .missingFields(Collections.emptyList())
                .followUpQuestions(List.of(
                        "What type of event are you planning?",
                        "Tell me about your event ideas",
                        "What's your budget range?"
                ))
                .suggestions(Map.of(
                        "eventTypes", List.of("BIRTHDAY_PARTY", "WEDDING", "CONFERENCE", "CORPORATE_EVENT", "FESTIVAL"),
                        "nextSteps", List.of("Choose event type", "Set date and location", "Plan guest list", "Create budget")
                ))
                .action(ShadeConversationResponse.ShadeAction.builder()
                        .type("none")
                        .ready(false)
                        .build())
                .timestamp(OffsetDateTime.now())
                .requiresConfirmation(false)
                .build();
    }

    private ShadeConversationResponse handleEventTypesQuery(AssistantSessionEntity session, ShadeConversationRequest request) {
        Map<String, String> eventTypes = getEventTypes();
        StringBuilder message = new StringBuilder("I can help you plan these types of events:\n\n");
        
        for (Map.Entry<String, String> eventType : eventTypes.entrySet()) {
            message.append("• **").append(eventType.getKey()).append("** - ").append(eventType.getValue()).append("\n");
        }
        
        message.append("\nEach event type has specific planning features, vendor recommendations, and timeline guidance. " +
                      "What type of event interests you most?");

        return ShadeConversationResponse.builder()
                .sessionId(session.getId())
                .message(message.toString())
                .intent("eventTypes")
                .collectedData(Map.of("availableEventTypes", eventTypes))
                .missingFields(Collections.emptyList())
                .followUpQuestions(List.of(
                        "Which event type interests you?",
                        "Tell me more about a specific event type",
                        "What's the occasion you're celebrating?"
                ))
                .suggestions(Map.of(
                        "eventTypes", new ArrayList<>(eventTypes.keySet()),
                        "nextSteps", List.of("Choose event type", "Set date", "Plan details")
                ))
                .action(ShadeConversationResponse.ShadeAction.builder()
                        .type("none")
                        .ready(false)
                        .build())
                .timestamp(OffsetDateTime.now())
                .requiresConfirmation(false)
                .build();
    }

    private ShadeConversationResponse handleEventTypeDetailsQuery(AssistantSessionEntity session, ShadeConversationRequest request) {
        String message = request.getMessage().toLowerCase();
        String eventType = "BIRTHDAY_PARTY"; // Default
        
        if (message.contains("wedding")) eventType = "WEDDING";
        else if (message.contains("conference")) eventType = "CONFERENCE";
        else if (message.contains("corporate")) eventType = "CORPORATE_EVENT";
        else if (message.contains("festival")) eventType = "FESTIVAL";
        else if (message.contains("anniversary")) eventType = "ANNIVERSARY";
        else if (message.contains("graduation")) eventType = "GRADUATION";
        else if (message.contains("holiday")) eventType = "HOLIDAY_PARTY";
        
        Map<String, Object> capabilities = getEventTypeCapabilities(eventType);
        
        StringBuilder responseMessage = new StringBuilder("Here's what I can help you with for **" + capabilities.get("eventTypeName") + "** events:\n\n");
        responseMessage.append("**Key Features:**\n");
        for (String feature : (List<String>) capabilities.get("keyFeatures")) {
            responseMessage.append("• ").append(feature).append("\n");
        }
        
        responseMessage.append("\n**Budget Categories:**\n");
        for (String category : (List<String>) capabilities.get("budgetCategories")) {
            responseMessage.append("• ").append(category).append("\n");
        }
        
        responseMessage.append("\n**Recommended Vendors:**\n");
        for (String vendor : (List<String>) capabilities.get("recommendedVendors")) {
            responseMessage.append("• ").append(vendor).append("\n");
        }
        
        responseMessage.append("\n**Timeline Milestones:**\n");
        for (Map<String, Object> milestone : (List<Map<String, Object>>) capabilities.get("timelineMonths")) {
            responseMessage.append("• ").append(milestone.get("title")).append(" (").append(milestone.get("daysBefore")).append(" days before)\n");
        }
        
        responseMessage.append("\nWould you like to start planning a ").append(((String) capabilities.get("eventTypeName")).toLowerCase()).append("?");

        return ShadeConversationResponse.builder()
                .sessionId(session.getId())
                .message(responseMessage.toString())
                .intent("eventTypeDetails")
                .collectedData(Map.of("eventType", eventType, "capabilities", capabilities))
                .missingFields(Collections.emptyList())
                .followUpQuestions(List.of(
                        "Start planning this event",
                        "Tell me about another event type",
                        "What's your budget range?"
                ))
                .suggestions(Map.of(
                        "nextSteps", List.of("Set date and location", "Plan guest list", "Create budget", "Find vendors"),
                        "eventTypes", List.of("BIRTHDAY_PARTY", "WEDDING", "CONFERENCE", "CORPORATE_EVENT")
                ))
                .action(ShadeConversationResponse.ShadeAction.builder()
                        .type("createEvent")
                        .arguments(Map.of("eventType", eventType))
                        .ready(false)
                        .build())
                .timestamp(OffsetDateTime.now())
                .requiresConfirmation(false)
                .build();
    }

    private ShadeConversationResponse handleEventManagementQuery(AssistantSessionEntity session, ShadeConversationRequest request, String intent) {
        String responseMessage;
        switch (intent) {
            case "updateEvent":
                responseMessage = "I'd be happy to help you update an existing event! " +
                        "However, I currently focus on creating new events. " +
                        "Would you like to create a new event instead?";
                break;
            case "listEvents":
                responseMessage = "I can help you plan and create new events! " +
                        "For viewing existing events, you might want to check your event dashboard. " +
                        "Would you like to create a new event?";
                break;
            case "deleteEvent":
                responseMessage = "I focus on helping you create amazing new events! " +
                        "For event management like deletion, you might want to use your event dashboard. " +
                        "Would you like to plan a new event instead?";
                break;
            default:
                responseMessage = "I can help you with event planning and creation. " +
                        "What type of event would you like to plan?";
        }

        return ShadeConversationResponse.builder()
                .sessionId(session.getId())
                .message(responseMessage)
                .intent(intent)
                .collectedData(Collections.emptyMap())
                .missingFields(Collections.emptyList())
                .followUpQuestions(List.of(
                        "Create a new event",
                        "Tell me about event types",
                        "What can you help me with?"
                ))
                .suggestions(Map.of(
                        "eventTypes", List.of("BIRTHDAY_PARTY", "WEDDING", "CONFERENCE", "CORPORATE_EVENT"),
                        "nextSteps", List.of("Choose event type", "Set date", "Plan details")
                ))
                .action(ShadeConversationResponse.ShadeAction.builder()
                        .type("createEvent")
                        .ready(false)
                        .build())
                .timestamp(OffsetDateTime.now())
                .requiresConfirmation(false)
                .build();
    }

    private ShadeConversationResponse handleOutOfScopeQuery(AssistantSessionEntity session, ShadeConversationRequest request) {
        return ShadeConversationResponse.builder()
                .sessionId(session.getId())
                .message("I'm sorry, I can only help you with event planning and event creation tasks. " +
                        "I can help you plan birthday parties, weddings, conferences, corporate events, and more! " +
                        "What type of event would you like to plan?")
                .intent("outOfScope")
                .collectedData(Collections.emptyMap())
                .missingFields(Collections.emptyList())
                .followUpQuestions(List.of(
                        "Plan a birthday party",
                        "Plan a wedding",
                        "Plan a conference",
                        "What types of events can you help with?"
                ))
                .suggestions(Map.of(
                        "eventTypes", List.of("BIRTHDAY_PARTY", "WEDDING", "CONFERENCE", "CORPORATE_EVENT", "FESTIVAL"),
                        "nextSteps", List.of("Choose event type", "Set date", "Plan details")
                ))
                .action(ShadeConversationResponse.ShadeAction.builder()
                        .type("none")
                        .ready(false)
                        .build())
                .timestamp(OffsetDateTime.now())
                .requiresConfirmation(false)
                .build();
    }

    // Essential methods moved from AssistantService for event creation focus
    
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "domains", List.of("EVENT"),
                "eventPlanning", true,
                "conversationalAI", true,
                "eventCreation", true,
                "supportedEventTypes", new ArrayList<>(getEventTypes().keySet())
        );
    }

    public Map<String, String> getEventTypes() {
        return Map.of(
                "CONFERENCE", "Professional gatherings focused on knowledge sharing and networking",
                "WEDDING", "Celebrations of love with curated experiences for guests",
                "BIRTHDAY_PARTY", "Personal celebrations for special milestones",
                "CORPORATE_EVENT", "Business events for team building and company milestones",
                "FESTIVAL", "Large scale festivals with multiple experiences and performances",
                "ANNIVERSARY", "Celebrations of important milestones and achievements",
                "GRADUATION", "Academic achievement celebrations",
                "HOLIDAY_PARTY", "Seasonal celebrations and holiday gatherings"
        );
    }

    public Map<String, Object> getEventTypeCapabilities(String eventType) {
        String upper = eventType.toUpperCase();
        switch (upper) {
            case "CONFERENCE":
                return Map.of(
                        "eventType", "CONFERENCE",
                        "eventTypeName", "Conference",
                        "description", "End-to-end planning support for professional conferences",
                        "keyFeatures", List.of(
                                "Agenda design and speaker coordination",
                                "Sponsor and exhibitor management",
                                "Attendee engagement workflows",
                                "Hybrid/virtual streaming setup guidance"
                        ),
                        "budgetCategories", List.of("Venue", "Production", "Technology", "Catering", "Marketing", "Hospitality"),
                        "recommendedVendors", List.of("AV Production", "Catering", "Registration Platform", "Security"),
                        "technologyNeeds", List.of("High-density WiFi", "LED walls", "Simultaneous translation"),
                        "timelineMonths", List.of(
                                Map.of("title", "Speaker outreach", "daysBefore", 90),
                                Map.of("title", "Sponsor confirmation", "daysBefore", 75),
                                Map.of("title", "Final agenda", "daysBefore", 30)
                        )
                );
            case "WEDDING":
                return Map.of(
                        "eventType", "WEDDING",
                        "eventTypeName", "Wedding",
                        "description", "Comprehensive planning for modern and traditional weddings",
                        "keyFeatures", List.of(
                                "Vendor curation and negotiation",
                                "Guest experience personalization",
                                "Ceremony and reception design",
                                "Cultural tradition integration"
                        ),
                        "budgetCategories", List.of("Venue", "Catering", "Decor", "Photography", "Entertainment", "Attire"),
                        "recommendedVendors", List.of("Florist", "Photographer", "Live Entertainment", "Cake Designer"),
                        "technologyNeeds", List.of("Lighting design", "Sound reinforcement", "Live streaming"),
                        "timelineMonths", List.of(
                                Map.of("title", "Venue selection", "daysBefore", 180),
                                Map.of("title", "Invitations sent", "daysBefore", 120),
                                Map.of("title", "Rehearsal", "daysBefore", 2)
                        )
                );
            case "BIRTHDAY_PARTY":
                return Map.of(
                        "eventType", "BIRTHDAY_PARTY",
                        "eventTypeName", "Birthday Party",
                        "description", "Fun and memorable birthday celebrations",
                        "keyFeatures", List.of(
                                "Theme selection and decoration",
                                "Entertainment and activities",
                                "Cake and catering coordination",
                                "Guest management and invitations"
                        ),
                        "budgetCategories", List.of("Venue", "Catering", "Decorations", "Entertainment", "Cake", "Party Favors"),
                        "recommendedVendors", List.of("Party Planner", "Caterer", "Entertainment", "Photographer"),
                        "technologyNeeds", List.of("Sound system", "Lighting", "Photo booth"),
                        "timelineMonths", List.of(
                                Map.of("title", "Theme selection", "daysBefore", 60),
                                Map.of("title", "Invitations sent", "daysBefore", 30),
                                Map.of("title", "Final preparations", "daysBefore", 7)
                        )
                );
            default:
                return Map.of(
                        "eventType", upper,
                        "eventTypeName", upper.charAt(0) + upper.substring(1).toLowerCase().replace('_', ' '),
                        "description", "Custom event support with curated recommendations",
                        "keyFeatures", List.of("Budget optimization", "Timeline automation", "Vendor matching"),
                        "budgetCategories", List.of("Venue", "Catering", "Logistics", "Experiences"),
                        "recommendedVendors", List.of("Catering", "Production", "Logistics"),
                        "technologyNeeds", List.of("Projector", "Sound system"),
                        "timelineMonths", List.of(
                                Map.of("title", "Planning kickoff", "daysBefore", 90),
                                Map.of("title", "Vendor confirmations", "daysBefore", 45)
                        )
                );
        }
    }
}
