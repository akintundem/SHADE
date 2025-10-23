# Event Planner Database Architecture

## Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    %% Core Entities
    UserAccount {
        UUID id PK
        String email UK
        String password
        String name
        UserStatus status
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    Event {
        UUID id PK
        String name
        UUID ownerId FK
        String description
        EventType eventType
        EventStatus eventStatus
        LocalDateTime startDateTime
        LocalDateTime endDateTime
        LocalDateTime registrationDeadline
        Integer capacity
        Integer currentAttendeeCount
        Boolean isPublic
        Boolean requiresApproval
        Boolean qrCodeEnabled
        String qrCode
        String coverImageUrl
        String eventWebsiteUrl
        String hashtag
        String theme
        String objectives
        String targetAudience
        String successMetrics
        String brandingGuidelines
        String venueRequirements
        String technicalRequirements
        String accessibilityFeatures
        String emergencyPlan
        String backupPlan
        String postEventTasks
        String metadata
        UUID venueId
        UUID platformPaymentId FK
        Boolean creationFeePaid
        BigDecimal creationFeeAmount
        LocalDateTime paymentDate
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Budget Management
    Budget {
        UUID id PK
        UUID eventId FK
        BigDecimal totalBudget
        String currency
        BigDecimal contingencyPercentage
        BigDecimal contingencyAmount
        BigDecimal totalEstimated
        BigDecimal totalActual
        BigDecimal variance
        BigDecimal variancePercentage
        String budgetStatus
        String approvedBy
        LocalDateTime approvedDate
        String notes
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    BudgetLineItem {
        UUID id PK
        UUID budgetId FK
        UUID organizationId
        String category
        String subcategory
        String description
        BigDecimal estimatedCost
        BigDecimal actualCost
        BigDecimal variance
        BigDecimal variancePercentage
        Integer quantity
        BigDecimal unitCost
        PlanningStatus planningStatus
        LocalDateTime bookingDate
        String contractReference
        String quoteReference
        String notes
        Boolean isEssential
        String priority
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    BudgetRevenue {
        UUID id PK
        UUID budgetId FK
        RevenueType revenueType
        String description
        BigDecimal estimatedAmount
        BigDecimal actualAmount
        BigDecimal receivedAmount
        BigDecimal pendingAmount
        String currency
        RevenueStatus status
        LocalDateTime receivedDate
        String notes
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Vendor Management
    EventVendor {
        UUID id PK
        UUID eventId FK
        String vendorName
        String legalName
        String vendorEmail
        String vendorPhone
        String websiteUrl
        String logoUrl
        OrganizationType organizationType
        String vendorDescription
        String vendorAddress
        String vendorCity
        String vendorState
        String vendorCountry
        String vendorPostalCode
        Double vendorLatitude
        Double vendorLongitude
        String googlePlaceId
        String businessLicense
        String taxId
        String insuranceInfo
        String certifications
        Double vendorRating
        Integer reviewCount
        String priceTier
        Boolean isVerified
        Boolean isActive
        VendorStatus vendorStatus
        String serviceCategory
        String serviceDescription
        BigDecimal quoteAmount
        BigDecimal finalAmount
        BigDecimal estimatedDeposit
        BigDecimal contractAmount
        BigDecimal totalEstimatedCost
        String contractUrl
        ContractStatus contractStatus
        QuoteStatus quoteStatus
        LocalDateTime contractSignedDate
        LocalDateTime rfpSentDate
        LocalDateTime quoteReceivedDate
        LocalDateTime bookingConfirmedDate
        LocalDateTime serviceDate
        Integer serviceDurationHours
        LocalDateTime setupTime
        LocalDateTime teardownTime
        String specialRequirements
        String notes
        Integer rating
        String review
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Attendee Management
    EventAttendance {
        UUID id PK
        UUID eventId FK
        UUID userId FK
        AttendanceStatus attendanceStatus
        LocalDateTime registrationDate
        LocalDateTime checkInTime
        LocalDateTime checkOutTime
        String qrCode
        Boolean qrCodeUsed
        LocalDateTime qrCodeUsedAt
        String ticketType
        Double ticketPrice
        Double registrationFee
        String dietaryRestrictions
        String accessibilityNeeds
        String emergencyContact
        String emergencyPhone
        String notes
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    EventUser {
        UUID id PK
        UUID eventId FK
        UUID userId FK
        EventUserType userType
        RegistrationStatus registrationStatus
        LocalDateTime registrationDate
        String notes
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    EventRole {
        UUID id PK
        UUID eventId FK
        UUID userId FK
        RoleName roleName
        String permissions
        Boolean isActive
        LocalDateTime assignedDate
        String notes
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Assistant Integration
    AssistantSessionEntity {
        UUID id PK
        UUID eventId FK UK
        UUID organizerId FK
        String domain
        String name
        SessionType type
        OffsetDateTime date
        String location
        Integer guestCount
        BigDecimal budget
        String description
        List preferences
        String notes
        SessionStatus status
        Boolean aiGenerated
        String contextPayload
        OffsetDateTime createdAt
        OffsetDateTime updatedAt
    }
    
    AssistantMessageEntity {
        UUID id PK
        UUID sessionId FK
        MessageRole role
        String content
        List followUpQuestions
        Map suggestions
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Timeline Management
    TimelineItem {
        UUID id PK
        UUID eventId FK
        String title
        String description
        LocalDateTime scheduledAt
        Integer durationMinutes
        LocalDateTime endTime
        ItemType itemType
        Status status
        String priority
        String location
        UUID assignedToUserId
        UUID assignedToOrganizationId
        String dependencies
        Integer setupTimeMinutes
        Integer teardownTimeMinutes
        String resourcesRequired
        String notes
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Checklist Management
    EventChecklist {
        UUID id PK
        UUID eventId FK
        String title
        String description
        Boolean isCompleted
        LocalDateTime dueDate
        String priority
        UUID assignedTo
        String category
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Program Management
    EventProgram {
        UUID id PK
        UUID eventId FK
        String title
        String description
        LocalDateTime startTime
        LocalDateTime endTime
        String location
        String speaker
        String sessionType
        Integer capacity
        Boolean isRequired
        Integer orderIndex
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Risk Management
    EventRisk {
        UUID id PK
        UUID eventId FK
        RiskType riskType
        Severity severity
        BigDecimal probability
        Integer impactScore
        Integer riskScore
        String title
        String description
        String mitigationPlan
        String contingencyPlan
        String assignedTo
        Status status
        LocalDateTime detectedAt
        LocalDateTime resolvedAt
        LocalDateTime reviewDate
        BigDecimal costImpact
        Integer scheduleImpactHours
        String notes
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Communication
    Communication {
        UUID id PK
        UUID eventId FK
        String channel
        String recipient
        String subject
        String content
        CommunicationStatus status
        LocalDateTime scheduledAt
        LocalDateTime sentAt
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    CommunicationLogEntity {
        UUID id PK
        String channel
        String recipient
        String subject
        String content
        CommunicationStatus status
        LocalDateTime createdAt
        String metadata
    }
    
    %% Admin & Platform Management
    PlatformPayment {
        UUID id PK
        UUID userId FK
        UUID eventId FK
        PlatformPaymentType paymentType
        BigDecimal amount
        String currency
        PlatformPaymentStatus status
        String paymentMethod
        String transactionId
        String paymentGatewayReference
        LocalDateTime paymentDate
        BigDecimal refundAmount
        LocalDateTime refundDate
        String refundReason
        String description
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    AdminDashboard {
        UUID id PK
        LocalDateTime date
        Long totalEvents
        Long activeEvents
        Long completedEvents
        Long cancelledEvents
        Long totalUsers
        Long newUsersToday
        Long activeUsersToday
        BigDecimal totalRevenue
        BigDecimal revenueToday
        BigDecimal revenueThisMonth
        BigDecimal averageRevenuePerEvent
        Long totalPayments
        Long successfulPayments
        Long failedPayments
        Long refundedPayments
        Long totalEventsCreated
        Long eventsCreatedToday
        BigDecimal averageEventsPerUser
        BigDecimal platformUptimePercentage
        String metadata
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Security & Authentication
    ClientApplication {
        UUID id PK
        String clientId UK
        String clientName
        String clientSecret
        String description
        String redirectUri
        String allowedOrigins
        Integer rateLimitPerHour
        Boolean isActive
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    UserSession {
        UUID id PK
        UUID userId FK
        String sessionToken
        LocalDateTime expiresAt
        String ipAddress
        String userAgent
        Boolean isActive
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }
    
    %% Relationships
    UserAccount ||--o{ Event : "owns"
    Event ||--o{ Budget : "has"
    Event ||--o{ EventVendor : "uses"
    Event ||--o{ EventAttendance : "has"
    Event ||--o{ EventUser : "has"
    Event ||--o{ EventRole : "defines"
    Event ||--|| AssistantSessionEntity : "has"
    Event ||--o{ TimelineItem : "has"
    Event ||--o{ EventChecklist : "has"
    Event ||--o{ EventProgram : "has"
    Event ||--o{ EventRisk : "has"
    Event ||--o{ Communication : "sends"
    Event ||--o{ PlatformPayment : "requires"
    
    Budget ||--o{ BudgetLineItem : "contains"
    Budget ||--o{ BudgetRevenue : "receives"
    
    UserAccount ||--o{ EventAttendance : "attends"
    UserAccount ||--o{ EventUser : "participates"
    UserAccount ||--o{ EventRole : "assigned"
    UserAccount ||--o{ AssistantSessionEntity : "creates"
    UserAccount ||--o{ PlatformPayment : "makes"
    UserAccount ||--o{ UserSession : "has"
    
    AssistantSessionEntity ||--o{ AssistantMessageEntity : "contains"
    
    PlatformPayment ||--o{ Event : "enables"
```

## Key Features

### 🎯 **Core Event Management**
- **Event**: Central entity with comprehensive event details
- **UserAccount**: User management with status tracking
- **EventAttendance**: Attendee management with QR codes
- **EventUser**: Event-specific user roles
- **EventRole**: Role-based permissions

### 💰 **Financial Management**
- **Budget**: Event budget planning
- **BudgetLineItem**: Detailed expense tracking (planning-focused)
- **BudgetRevenue**: Revenue source tracking
- **PlatformPayment**: Event creation fees and premium features

### 🏢 **Vendor Management**
- **EventVendor**: Complete vendor information and event relationships
- **Consolidated**: All vendor data in one entity for simplicity
- **Planning-focused**: No payment processing, just coordination

### 🤖 **AI Assistant Integration**
- **AssistantSessionEntity**: AI conversation sessions (ONE per event)
- **AssistantMessageEntity**: Individual messages in conversations
- **Constraint**: Each event has exactly one chat session

### 📅 **Timeline & Communication**
- **TimelineItem**: Event timeline and run-of-show management
- **Communication**: Scheduled communications
- **CommunicationLogEntity**: Communication history

### ✅ **Checklist & Program Management**
- **EventChecklist**: Task management and completion tracking
- **EventProgram**: Event agenda and session management
- **EventRisk**: Risk assessment and mitigation planning

### 👑 **Admin Platform Management**
- **AdminDashboard**: Platform metrics and analytics
- **PlatformPayment**: Revenue tracking for platform
- **ClientApplication**: API client management

### 🔒 **Security & Authentication**
- **UserSession**: User session management
- **ClientApplication**: API client validation

## ENUMs Used

- **UserStatus**: ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION, DELETED
- **EventStatus**: PLANNING, ACTIVE, COMPLETED, CANCELLED, POSTPONED
- **EventType**: CONFERENCE, WORKSHOP, MEETING, SEMINAR, etc.
- **PlanningStatus**: PLANNED, QUOTED, BOOKED, IN_PROGRESS, COMPLETED, CANCELLED, ON_HOLD
- **RevenueType**: TICKET_SALES, SPONSORSHIP, DONATION, GRANT, etc.
- **RevenueStatus**: PENDING, CONFIRMED, RECEIVED, PARTIALLY_RECEIVED, OVERDUE, CANCELLED, REFUNDED
- **VendorStatus**: INQUIRY, RFP_SENT, QUOTED, NEGOTIATING, BOOKED, IN_PROGRESS, COMPLETED, CANCELLED, NO_RESPONSE
- **ContractStatus**: DRAFT, SENT, REVIEWED, SIGNED, EXECUTED, EXPIRED, TERMINATED
- **QuoteStatus**: REQUESTED, RECEIVED, REVIEWED, ACCEPTED, REJECTED, EXPIRED, NEGOTIATING
- **AttendanceStatus**: REGISTERED, CHECKED_IN, CHECKED_OUT, NO_SHOW, CANCELLED
- **EventUserType**: ORGANIZER, COORDINATOR, ATTENDEE, VOLUNTEER, VENDOR, SPEAKER, SPONSOR, MEDIA, STAFF
- **RegistrationStatus**: PENDING, CONFIRMED, CANCELLED, WAITLISTED, REJECTED, TRANSFERRED
- **RoleName**: ORGANIZER, COORDINATOR, VOLUNTEER, VENDOR, SPEAKER, SPONSOR, MEDIA, STAFF
- **SessionType**: EVENT_PLANNING, BUDGET_PLANNING, VENDOR_MANAGEMENT, ATTENDEE_MANAGEMENT, etc.
- **SessionStatus**: ACTIVE, PAUSED, COMPLETED, ARCHIVED, CANCELLED
- **MessageRole**: USER, ASSISTANT, SYSTEM, ADMIN
- **TimelineStatus**: PENDING, IN_PROGRESS, COMPLETED, CANCELLED, POSTPONED, OVERDUE
- **CommunicationStatus**: PENDING, SENT, DELIVERED, FAILED, BOUNCED, OPENED, CLICKED, REPLIED
- **PlatformPaymentStatus**: PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, PARTIALLY_REFUNDED, CANCELLED, EXPIRED
- **PlatformPaymentType**: EVENT_CREATION_FEE, PREMIUM_FEATURES, ADDITIONAL_STORAGE, PRIORITY_SUPPORT, MARKETING_BOOST, ANALYTICS_UPGRADE
- **ItemType**: SESSION, BREAK, MEAL, NETWORKING, KEYNOTE, WORKSHOP, PANEL, Q_AND_A, CLOSING
- **RiskType**: WEATHER, VENUE, VENDOR, ATTENDEE, TECHNICAL, FINANCIAL, SECURITY, HEALTH, TRANSPORTATION, OTHER
- **Severity**: LOW, MEDIUM, HIGH, CRITICAL
- **Status**: PENDING, IN_PROGRESS, COMPLETED, CANCELLED, POSTPONED, OVERDUE

## Architecture Highlights

✅ **Planning-Focused**: No payment processing, just event planning assistance
✅ **Admin Platform**: Complete revenue tracking and user management
✅ **Type Safety**: Comprehensive ENUM usage throughout
✅ **Scalable**: Proper relationships and indexing
✅ **Secure**: Role-based access and session management
✅ **AI-Ready**: Assistant integration for conversational planning