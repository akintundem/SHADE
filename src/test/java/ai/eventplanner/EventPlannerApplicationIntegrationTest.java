package ai.eventplanner;

import ai.eventplanner.comms.repository.CommunicationLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest
@AutoConfigureMockMvc
class EventPlannerApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Test
    @DisplayName("Event lifecycle: create, fetch, delete")
    void eventLifecycle() throws Exception {
        UUID organizerId = UUID.randomUUID();
        JsonNode created = createEvent("Launch Event", organizerId);

        UUID eventId = UUID.fromString(created.get("id").asText());
        assertThat(created.get("organizerId").asText()).isEqualTo(organizerId.toString());

        var getResponse = mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode fetched = objectMapper.readTree(getResponse.getResponse().getContentAsString());
        assertThat(fetched.get("name").asText()).isEqualTo("Launch Event");

        mockMvc.perform(delete("/api/v1/events/{id}", eventId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Attendee workflow supports RSVP updates and check-ins")
    void attendeeWorkflow() throws Exception {
        UUID eventId = UUID.fromString(createEvent("Gala Night", UUID.randomUUID()).get("id").asText());

        var payload = objectMapper.writeValueAsString(List.of(
                Map.of(
                        "eventId", eventId.toString(),
                        "name", "Ada Lovelace",
                        "email", "ada@example.com",
                        "phone", "+15555550101"
                )
        ));

        var addResponse = mockMvc.perform(post("/api/v1/attendees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode attendees = objectMapper.readTree(addResponse.getResponse().getContentAsString());
        assertThat(attendees).hasSize(1);
        String attendeeId = attendees.get(0).get("id").asText();

        var listResponse = mockMvc.perform(get("/api/v1/attendees/event/{eventId}", eventId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listed = objectMapper.readTree(listResponse.getResponse().getContentAsString());
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).get("rsvpStatus").asText()).isEqualTo("pending");

        mockMvc.perform(patch("/api/v1/attendees/{id}/rsvp", attendeeId).param("status", "confirmed"))
                .andExpect(status().isOk());

        var checkInResponse = mockMvc.perform(post("/api/v1/attendees/{id}/check-in", attendeeId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode checkedIn = objectMapper.readTree(checkInResponse.getResponse().getContentAsString());
        assertThat(checkedIn.get("checkedInAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Budget operations calculate rollups using actual cost when available")
    void budgetOperations() throws Exception {
        UUID eventId = UUID.fromString(createEvent("Summit", UUID.randomUUID()).get("id").asText());

        String budgetPayload = objectMapper.writeValueAsString(Map.of(
                "eventId", eventId.toString(),
                "totalBudget", BigDecimal.valueOf(25000),
                "currency", "USD"
        ));

        var budgetResponse = mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetPayload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode budget = objectMapper.readTree(budgetResponse.getResponse().getContentAsString());
        String budgetId = budget.get("id").asText();

        String lineItemPayload = objectMapper.writeValueAsString(Map.of(
                "budgetId", budgetId,
                "category", "AV",
                "description", "Audio equipment",
                "estimatedCost", BigDecimal.valueOf(5000),
                "actualCost", BigDecimal.valueOf(4800)
        ));

        mockMvc.perform(post("/api/v1/budgets/{eventId}/line-items", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineItemPayload))
                .andExpect(status().isOk());

        var rollupResponse = mockMvc.perform(get("/api/v1/budgets/{budgetId}/rollup", budgetId))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(rollupResponse.getResponse().getContentAsString()).contains("4800");
    }

    @Test
    @DisplayName("Timeline items can be created and retrieved in chronological order")
    void timelineOperations() throws Exception {
        UUID eventId = UUID.fromString(createEvent("Product Expo", UUID.randomUUID()).get("id").asText());

        String timelinePayload = objectMapper.writeValueAsString(Map.of(
                "eventId", eventId.toString(),
                "title", "Doors Open",
                "description", "Guests begin arrival",
                "scheduledAt", LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0).toString(),
                "durationMinutes", 60
        ));

        mockMvc.perform(post("/api/v1/timeline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(timelinePayload))
                .andExpect(status().isOk());

        var listResponse = mockMvc.perform(get("/api/v1/timeline/{eventId}", eventId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode timelineItems = objectMapper.readTree(listResponse.getResponse().getContentAsString());
        assertThat(timelineItems).hasSize(1);
        assertThat(timelineItems.get(0).get("title").asText()).isEqualTo("Doors Open");
    }

    @Test
    @DisplayName("Risk endpoint returns heuristic data when external provider unavailable")
    void riskEndpoint() throws Exception {
        UUID eventId = UUID.fromString(createEvent("Conference", UUID.randomUUID()).get("id").asText());

        var asyncResult = mockMvc.perform(get("/api/v1/risks/{eventId}", eventId)
                        .param("lat", "40.7128")
                        .param("lon", "-74.0060"))
                .andExpect(request().asyncStarted())
                .andReturn();

        var response = mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andReturn();

        String raw = response.getResponse().getContentAsString();
        JsonNode risks = objectMapper.readTree(raw);
        assertThat(risks.size()).isGreaterThanOrEqualTo(3);
        assertThat(risks.get(0).get("riskId").asText()).isNotBlank();
        assertThat(risks.get(0).get("riskLevel").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Payment intent falls back to local mode when Stripe is not configured")
    void paymentIntentFallback() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "amountCents", 12500,
                "currency", "usd",
                "userId", UUID.randomUUID().toString()
        ));

        var response = mockMvc.perform(post("/api/v1/payments/intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode intent = objectMapper.readTree(response.getResponse().getContentAsString());
        assertThat(intent.get("livemode").asBoolean()).isFalse();
        assertThat(intent.get("clientSecret").asText()).startsWith("secret_local_");
    }

    @Test
    @DisplayName("Comms endpoints queue email requests and persist audit logs")
    void commsQueueEmail() throws Exception {
        communicationLogRepository.deleteAll();

        String payload = objectMapper.writeValueAsString(Map.of(
                "to", "events@example.com",
                "subject", "Welcome",
                "body", "Thanks for registering!"
        ));

        var response = mockMvc.perform(post("/api/v1/comms/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        assertThat(body.get("channel").asText()).isEqualTo("email");
        assertThat(body.get("status").asText()).isIn("queued", "sent");
        assertThat(communicationLogRepository.count()).isEqualTo(1);
    }

    private JsonNode createEvent(String name, UUID organizerId) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "organizationId", UUID.randomUUID().toString(),
                "name", name,
                "type", "conference",
                "date", LocalDateTime.now().plusDays(30).withNano(0).toString(),
                "metadata", Map.of("capacity", 150)
        ));

        var response = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", organizerId.toString())
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(response.getResponse().getContentAsString());
    }
}
