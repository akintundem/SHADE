package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for comprehensive event plans
 */
@Schema(description = "Comprehensive event plan response")
public class EventPlanResponse {

    @Schema(description = "Unique identifier of the event", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID eventId;

    @Schema(description = "Name of the event", example = "Annual Company Conference")
    private String eventName;

    @Schema(description = "Type of the event", example = "CONFERENCE")
    private String eventType;

    @Schema(description = "Date and time of the event", example = "2024-06-15T09:00:00")
    private LocalDateTime date;

    @Schema(description = "Location of the event", example = "San Francisco, CA")
    private String location;

    @Schema(description = "Expected number of guests", example = "200")
    private Integer guestCount;

    @Schema(description = "Event budget in USD", example = "50000.0")
    private Double budget;

    @Schema(description = "Event recommendations and best practices")
    private List<String> recommendations;

    @Schema(description = "Detailed budget breakdown by category")
    private Map<String, Double> budgetBreakdown;

    @Schema(description = "Event planning timeline with key milestones")
    private List<String> timeline;

    @Schema(description = "Recommended vendors and service providers")
    private List<String> vendorRecommendations;

    @Schema(description = "Next steps for event planning")
    private List<String> nextSteps;

    @Schema(description = "Risk assessment and mitigation strategies")
    private List<RiskAssessmentResponse> riskAssessment;

    @Schema(description = "Success metrics and KPIs")
    private List<SuccessMetricResponse> successMetrics;

    @Schema(description = "Cost-saving tips and recommendations")
    private List<String> costSavingTips;

    @Schema(description = "Critical milestones and deadlines")
    private List<String> criticalMilestones;

    @Schema(description = "Dependencies between planning tasks")
    private List<DependencyResponse> dependencies;

    @Schema(description = "When the plan was generated", example = "2024-01-15T10:30:00")
    private LocalDateTime generatedAt;

    @Schema(description = "Status of the plan generation", example = "completed")
    private String status;

    @Schema(description = "Additional notes or considerations")
    private String notes;

    // Constructors
    public EventPlanResponse() {}

    public EventPlanResponse(UUID eventId, String eventName, String eventType, LocalDateTime date, String location, Integer guestCount, Double budget) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventType = eventType;
        this.date = date;
        this.location = location;
        this.guestCount = guestCount;
        this.budget = budget;
        this.generatedAt = LocalDateTime.now();
        this.status = "completed";
    }

    // Getters and setters
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public Map<String, Double> getBudgetBreakdown() {
        return budgetBreakdown;
    }

    public void setBudgetBreakdown(Map<String, Double> budgetBreakdown) {
        this.budgetBreakdown = budgetBreakdown;
    }

    public List<String> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<String> timeline) {
        this.timeline = timeline;
    }

    public List<String> getVendorRecommendations() {
        return vendorRecommendations;
    }

    public void setVendorRecommendations(List<String> vendorRecommendations) {
        this.vendorRecommendations = vendorRecommendations;
    }

    public List<String> getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(List<String> nextSteps) {
        this.nextSteps = nextSteps;
    }

    public List<RiskAssessmentResponse> getRiskAssessment() {
        return riskAssessment;
    }

    public void setRiskAssessment(List<RiskAssessmentResponse> riskAssessment) {
        this.riskAssessment = riskAssessment;
    }

    public List<SuccessMetricResponse> getSuccessMetrics() {
        return successMetrics;
    }

    public void setSuccessMetrics(List<SuccessMetricResponse> successMetrics) {
        this.successMetrics = successMetrics;
    }

    public List<String> getCostSavingTips() {
        return costSavingTips;
    }

    public void setCostSavingTips(List<String> costSavingTips) {
        this.costSavingTips = costSavingTips;
    }

    public List<String> getCriticalMilestones() {
        return criticalMilestones;
    }

    public void setCriticalMilestones(List<String> criticalMilestones) {
        this.criticalMilestones = criticalMilestones;
    }

    public List<DependencyResponse> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DependencyResponse> dependencies) {
        this.dependencies = dependencies;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Nested response classes
    @Schema(description = "Risk assessment information")
    public static class RiskAssessmentResponse {
        @Schema(description = "Risk description", example = "Speaker cancellation")
        private String risk;

        @Schema(description = "Risk probability", example = "Medium")
        private String probability;

        @Schema(description = "Mitigation strategy", example = "Have backup speakers and flexible agenda")
        private String mitigation;

        // Constructors
        public RiskAssessmentResponse() {}

        public RiskAssessmentResponse(String risk, String probability, String mitigation) {
            this.risk = risk;
            this.probability = probability;
            this.mitigation = mitigation;
        }

        // Getters and setters
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }
        public String getProbability() { return probability; }
        public void setProbability(String probability) { this.probability = probability; }
        public String getMitigation() { return mitigation; }
        public void setMitigation(String mitigation) { this.mitigation = mitigation; }
    }

    @Schema(description = "Success metric information")
    public static class SuccessMetricResponse {
        @Schema(description = "Metric name", example = "Attendance rate")
        private String metric;

        @Schema(description = "Target value", example = "80% of invited attendees")
        private String target;

        // Constructors
        public SuccessMetricResponse() {}

        public SuccessMetricResponse(String metric, String target) {
            this.metric = metric;
            this.target = target;
        }

        // Getters and setters
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }

    @Schema(description = "Dependency information")
    public static class DependencyResponse {
        @Schema(description = "Prerequisite task", example = "Venue booking")
        private String prerequisite;

        @Schema(description = "Dependent task", example = "Speaker confirmation")
        private String dependent;

        @Schema(description = "Dependency description", example = "Venue must be confirmed before speaker coordination")
        private String description;

        // Constructors
        public DependencyResponse() {}

        public DependencyResponse(String prerequisite, String dependent, String description) {
            this.prerequisite = prerequisite;
            this.dependent = dependent;
            this.description = description;
        }

        // Getters and setters
        public String getPrerequisite() { return prerequisite; }
        public void setPrerequisite(String prerequisite) { this.prerequisite = prerequisite; }
        public String getDependent() { return dependent; }
        public void setDependent(String dependent) { this.dependent = dependent; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
