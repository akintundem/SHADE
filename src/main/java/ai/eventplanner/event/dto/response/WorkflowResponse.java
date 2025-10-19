package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for workflow orchestration
 */
@Schema(description = "Workflow orchestration response")
public class WorkflowResponse {

    @Schema(description = "Unique identifier of the event", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID eventId;

    @Schema(description = "Type of the event", example = "CONFERENCE")
    private String eventType;

    @Schema(description = "Status of the workflow", example = "completed")
    private String status;

    @Schema(description = "Status message", example = "Comprehensive event plan generated successfully!")
    private String message;

    @Schema(description = "Base event plan information")
    private EventPlanResponse basePlan;

    @Schema(description = "Budget planning details")
    private BudgetPlanResponse budgetPlan;

    @Schema(description = "Vendor planning details")
    private VendorPlanResponse vendorPlan;

    @Schema(description = "Timeline planning details")
    private TimelinePlanResponse timelinePlan;

    @Schema(description = "Logistics planning details")
    private LogisticsPlanResponse logisticsPlan;

    @Schema(description = "Event-specific planning details")
    private EventSpecificPlanResponse eventSpecificPlan;

    @Schema(description = "Next steps for event planning")
    private List<String> nextSteps;

    @Schema(description = "Recommendations and best practices")
    private List<String> recommendations;

    @Schema(description = "Risk assessment and mitigation strategies")
    private List<RiskAssessmentResponse> riskAssessment;

    @Schema(description = "Success metrics and KPIs")
    private List<SuccessMetricResponse> successMetrics;

    @Schema(description = "When the workflow was completed", example = "2024-01-15T10:30:00")
    private LocalDateTime completedAt;

    @Schema(description = "Total processing time in milliseconds", example = "5000")
    private Long processingTimeMs;

    @Schema(description = "Additional metadata about the workflow")
    private String metadata;

    // Constructors
    public WorkflowResponse() {}

    public WorkflowResponse(UUID eventId, String eventType, String status, String message) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.status = status;
        this.message = message;
        this.completedAt = LocalDateTime.now();
    }

    // Getters and setters
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EventPlanResponse getBasePlan() {
        return basePlan;
    }

    public void setBasePlan(EventPlanResponse basePlan) {
        this.basePlan = basePlan;
    }

    public BudgetPlanResponse getBudgetPlan() {
        return budgetPlan;
    }

    public void setBudgetPlan(BudgetPlanResponse budgetPlan) {
        this.budgetPlan = budgetPlan;
    }

    public VendorPlanResponse getVendorPlan() {
        return vendorPlan;
    }

    public void setVendorPlan(VendorPlanResponse vendorPlan) {
        this.vendorPlan = vendorPlan;
    }

    public TimelinePlanResponse getTimelinePlan() {
        return timelinePlan;
    }

    public void setTimelinePlan(TimelinePlanResponse timelinePlan) {
        this.timelinePlan = timelinePlan;
    }

    public LogisticsPlanResponse getLogisticsPlan() {
        return logisticsPlan;
    }

    public void setLogisticsPlan(LogisticsPlanResponse logisticsPlan) {
        this.logisticsPlan = logisticsPlan;
    }

    public EventSpecificPlanResponse getEventSpecificPlan() {
        return eventSpecificPlan;
    }

    public void setEventSpecificPlan(EventSpecificPlanResponse eventSpecificPlan) {
        this.eventSpecificPlan = eventSpecificPlan;
    }

    public List<String> getNextSteps() {
        return nextSteps;
    }

    public void setNextSteps(List<String> nextSteps) {
        this.nextSteps = nextSteps;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    // Nested response classes
    @Schema(description = "Budget plan response")
    public static class BudgetPlanResponse {
        @Schema(description = "Event type", example = "CONFERENCE")
        private String eventType;

        @Schema(description = "Total budget", example = "50000.0")
        private Double totalBudget;

        @Schema(description = "Budget breakdown by category")
        private Map<String, Double> budgetBreakdown;

        @Schema(description = "Cost-saving tips")
        private List<String> costSavingTips;

        @Schema(description = "Budget timeline")
        private List<String> budgetTimeline;

        @Schema(description = "Payment schedule")
        private List<String> paymentSchedule;

        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public Double getTotalBudget() { return totalBudget; }
        public void setTotalBudget(Double totalBudget) { this.totalBudget = totalBudget; }
        public Map<String, Double> getBudgetBreakdown() { return budgetBreakdown; }
        public void setBudgetBreakdown(Map<String, Double> budgetBreakdown) { this.budgetBreakdown = budgetBreakdown; }
        public List<String> getCostSavingTips() { return costSavingTips; }
        public void setCostSavingTips(List<String> costSavingTips) { this.costSavingTips = costSavingTips; }
        public List<String> getBudgetTimeline() { return budgetTimeline; }
        public void setBudgetTimeline(List<String> budgetTimeline) { this.budgetTimeline = budgetTimeline; }
        public List<String> getPaymentSchedule() { return paymentSchedule; }
        public void setPaymentSchedule(List<String> paymentSchedule) { this.paymentSchedule = paymentSchedule; }
    }

    @Schema(description = "Vendor plan response")
    public static class VendorPlanResponse {
        @Schema(description = "Event type", example = "CONFERENCE")
        private String eventType;

        @Schema(description = "Event location", example = "San Francisco, CA")
        private String location;

        @Schema(description = "Vendor categories")
        private List<String> vendorCategories;

        @Schema(description = "Vendor recommendations")
        private List<VendorRecommendationResponse> vendorRecommendations;

        @Schema(description = "Vendor timeline")
        private List<String> vendorTimeline;

        @Schema(description = "Vendor checklist")
        private List<String> vendorChecklist;

        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public List<String> getVendorCategories() { return vendorCategories; }
        public void setVendorCategories(List<String> vendorCategories) { this.vendorCategories = vendorCategories; }
        public List<VendorRecommendationResponse> getVendorRecommendations() { return vendorRecommendations; }
        public void setVendorRecommendations(List<VendorRecommendationResponse> vendorRecommendations) { this.vendorRecommendations = vendorRecommendations; }
        public List<String> getVendorTimeline() { return vendorTimeline; }
        public void setVendorTimeline(List<String> vendorTimeline) { this.vendorTimeline = vendorTimeline; }
        public List<String> getVendorChecklist() { return vendorChecklist; }
        public void setVendorChecklist(List<String> vendorChecklist) { this.vendorChecklist = vendorChecklist; }
    }

    @Schema(description = "Timeline plan response")
    public static class TimelinePlanResponse {
        @Schema(description = "Event type", example = "CONFERENCE")
        private String eventType;

        @Schema(description = "Event date", example = "2024-06-15T09:00:00")
        private LocalDateTime eventDate;

        @Schema(description = "Pre-event timeline")
        private List<TimelineItemResponse> preEventTimeline;

        @Schema(description = "Day-of timeline")
        private List<TimelineItemResponse> dayOfTimeline;

        @Schema(description = "Post-event timeline")
        private List<TimelineItemResponse> postEventTimeline;

        @Schema(description = "Critical milestones")
        private List<String> criticalMilestones;

        @Schema(description = "Task dependencies")
        private List<DependencyResponse> dependencies;

        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public LocalDateTime getEventDate() { return eventDate; }
        public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
        public List<TimelineItemResponse> getPreEventTimeline() { return preEventTimeline; }
        public void setPreEventTimeline(List<TimelineItemResponse> preEventTimeline) { this.preEventTimeline = preEventTimeline; }
        public List<TimelineItemResponse> getDayOfTimeline() { return dayOfTimeline; }
        public void setDayOfTimeline(List<TimelineItemResponse> dayOfTimeline) { this.dayOfTimeline = dayOfTimeline; }
        public List<TimelineItemResponse> getPostEventTimeline() { return postEventTimeline; }
        public void setPostEventTimeline(List<TimelineItemResponse> postEventTimeline) { this.postEventTimeline = postEventTimeline; }
        public List<String> getCriticalMilestones() { return criticalMilestones; }
        public void setCriticalMilestones(List<String> criticalMilestones) { this.criticalMilestones = criticalMilestones; }
        public List<DependencyResponse> getDependencies() { return dependencies; }
        public void setDependencies(List<DependencyResponse> dependencies) { this.dependencies = dependencies; }
    }

    @Schema(description = "Logistics plan response")
    public static class LogisticsPlanResponse {
        @Schema(description = "Event type", example = "CONFERENCE")
        private String eventType;

        @Schema(description = "Venue requirements")
        private List<String> venueRequirements;

        @Schema(description = "Transportation needs")
        private List<String> transportationNeeds;

        @Schema(description = "Catering requirements")
        private List<String> cateringRequirements;

        @Schema(description = "Technical requirements")
        private List<String> technicalRequirements;

        @Schema(description = "Staffing needs")
        private List<String> staffingNeeds;

        @Schema(description = "Safety considerations")
        private List<String> safetyConsiderations;

        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public List<String> getVenueRequirements() { return venueRequirements; }
        public void setVenueRequirements(List<String> venueRequirements) { this.venueRequirements = venueRequirements; }
        public List<String> getTransportationNeeds() { return transportationNeeds; }
        public void setTransportationNeeds(List<String> transportationNeeds) { this.transportationNeeds = transportationNeeds; }
        public List<String> getCateringRequirements() { return cateringRequirements; }
        public void setCateringRequirements(List<String> cateringRequirements) { this.cateringRequirements = cateringRequirements; }
        public List<String> getTechnicalRequirements() { return technicalRequirements; }
        public void setTechnicalRequirements(List<String> technicalRequirements) { this.technicalRequirements = technicalRequirements; }
        public List<String> getStaffingNeeds() { return staffingNeeds; }
        public void setStaffingNeeds(List<String> staffingNeeds) { this.staffingNeeds = staffingNeeds; }
        public List<String> getSafetyConsiderations() { return safetyConsiderations; }
        public void setSafetyConsiderations(List<String> safetyConsiderations) { this.safetyConsiderations = safetyConsiderations; }
    }

    @Schema(description = "Event-specific plan response")
    public static class EventSpecificPlanResponse {
        @Schema(description = "Event type", example = "CONFERENCE")
        private String eventType;

        @Schema(description = "Event-specific details")
        private Map<String, Object> eventSpecifics;

        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public Map<String, Object> getEventSpecifics() { return eventSpecifics; }
        public void setEventSpecifics(Map<String, Object> eventSpecifics) { this.eventSpecifics = eventSpecifics; }
    }

    // Supporting response classes
    @Schema(description = "Vendor recommendation response")
    public static class VendorRecommendationResponse {
        @Schema(description = "Vendor name", example = "Professional Venue")
        private String name;

        @Schema(description = "Vendor category", example = "venue")
        private String category;

        @Schema(description = "Vendor description", example = "High-end business venues with meeting rooms")
        private String description;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    @Schema(description = "Timeline item response")
    public static class TimelineItemResponse {
        @Schema(description = "Timeframe", example = "8-12 weeks before")
        private String timeframe;

        @Schema(description = "Task description", example = "Book venue and confirm date")
        private String task;

        @Schema(description = "Priority level", example = "High")
        private String priority;

        @Schema(description = "Detailed description", example = "Venue booking and date confirmation")
        private String description;

        // Getters and setters
        public String getTimeframe() { return timeframe; }
        public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    @Schema(description = "Risk assessment response")
    public static class RiskAssessmentResponse {
        @Schema(description = "Risk description", example = "Speaker cancellation")
        private String risk;

        @Schema(description = "Risk probability", example = "Medium")
        private String probability;

        @Schema(description = "Mitigation strategy", example = "Have backup speakers and flexible agenda")
        private String mitigation;

        // Getters and setters
        public String getRisk() { return risk; }
        public void setRisk(String risk) { this.risk = risk; }
        public String getProbability() { return probability; }
        public void setProbability(String probability) { this.probability = probability; }
        public String getMitigation() { return mitigation; }
        public void setMitigation(String mitigation) { this.mitigation = mitigation; }
    }

    @Schema(description = "Success metric response")
    public static class SuccessMetricResponse {
        @Schema(description = "Metric name", example = "Attendance rate")
        private String metric;

        @Schema(description = "Target value", example = "80% of invited attendees")
        private String target;

        // Getters and setters
        public String getMetric() { return metric; }
        public void setMetric(String metric) { this.metric = metric; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }

    @Schema(description = "Dependency response")
    public static class DependencyResponse {
        @Schema(description = "Prerequisite task", example = "Venue booking")
        private String prerequisite;

        @Schema(description = "Dependent task", example = "Speaker confirmation")
        private String dependent;

        @Schema(description = "Dependency description", example = "Venue must be confirmed before speaker coordination")
        private String description;

        // Getters and setters
        public String getPrerequisite() { return prerequisite; }
        public void setPrerequisite(String prerequisite) { this.prerequisite = prerequisite; }
        public String getDependent() { return dependent; }
        public void setDependent(String dependent) { this.dependent = dependent; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
