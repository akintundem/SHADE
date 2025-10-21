# Shade AI LangGraph Architecture

## Updated System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        A[Web Browser]
        B[Mobile App]
        C[API Client]
    end
    
    subgraph "API Gateway"
        D[FastAPI Server]
        E[Chat Endpoint /chat]
        F[Dashboard /]
        G[OpenAPI Docs /docs]
    end
    
    subgraph "LangGraph Flow Manager"
        H[Routing Node]
        I[Domain Agents Layer]
        J[Aggregator Node]
        K[Shared Context Memory]
        L[Observability Hooks]
    end
    
    subgraph "Domain Agents"
        M[Event Agent]
        N[Budget Agent]
        O[Venue Agent]
        P[Vendor Agent]
        Q[Risk Agent]
        R[Attendee Agent]
        S[Weather Agent]
        T[Outreach Agent]
    end
    
    subgraph "Knowledge Layer (RAG Gateway)"
        U[Vector DB (Chroma / Pinecone)]
        V[Document Loader + Embedding Pipeline]
    end
    
    subgraph "Toolkits & External APIs"
        W[Google APIs (Gmail, Calendar)]
        X[Weather API]
        Y[Tavily / SerpAPI]
        Z[Payment / Booking APIs]
    end
    
    subgraph "Data Layer"
        DB[MongoDB / PostgreSQL]
        CACHE[Redis / In-memory Cache]
        TASKQ[Async Worker Queue]
    end
    
    subgraph "Monitoring"
        LS[LangSmith / Tracing]
        GF[Grafana / Logs]
    end
    
    A --> D
    B --> D
    C --> D
    D --> H
    H --> I
    I --> M & N & O & P & Q & R & S & T
    M & N & O & P & Q & R & S & T --> U
    U --> DB
    I --> K
    K --> J
    I --> W & X & Y & Z
    J --> D
    H & I & J --> LS
    LS --> GF
    DB --> CACHE
    T --> TASKQ
```

## LangGraph Flow Architecture

```mermaid
stateDiagram-v2
    [*] --> RequestReceived
    RequestReceived --> RoutingNode
    RoutingNode --> SingleDomain
    RoutingNode --> MultiDomain
    
    SingleDomain --> DomainProcessing
    MultiDomain --> DomainProcessing
    
    DomainProcessing --> AggregatorNode
    AggregatorNode --> ResponseGeneration
    ResponseGeneration --> [*]
    
    note right of RoutingNode
        Intelligent routing based on
        message content and context
    end note
    
    note right of DomainProcessing
        Parallel or sequential
        domain agent execution
    end note
    
    note right of AggregatorNode
        Synthesize responses from
        multiple domain agents
    end note
```

## Domain Agent Specialization

```mermaid
graph LR
    subgraph "Event Planning Domain"
        A[Event Agent]
        B[Budget Agent]
        C[Venue Agent]
        D[Vendor Agent]
    end
    
    subgraph "Risk & Safety Domain"
        E[Risk Agent]
        F[Weather Agent]
    end
    
    subgraph "People Domain"
        G[Attendee Agent]
        H[Outreach Agent]
    end
    
    subgraph "Knowledge Base"
        I[Event RAG]
        J[Budget RAG]
        K[Venue RAG]
        L[Vendor RAG]
        M[Risk RAG]
        N[Weather RAG]
        O[Attendee RAG]
        P[Communication RAG]
    end
    
    A --> I
    B --> J
    C --> K
    D --> L
    E --> M
    F --> N
    G --> O
    H --> P
```

## RAG Gateway Architecture

```mermaid
graph TB
    subgraph "RAG Gateway"
        A[Query Interface]
        B[Domain Router]
        C[Vector Search]
        D[Context Synthesizer]
    end
    
    subgraph "Vector Store"
        E[Event Collection]
        F[Budget Collection]
        G[Venue Collection]
        H[Vendor Collection]
        I[Risk Collection]
        J[Weather Collection]
        K[Attendee Collection]
        L[Communication Collection]
    end
    
    subgraph "Embedding Pipeline"
        M[Text Preprocessing]
        N[Embedding Generation]
        O[Vector Indexing]
    end
    
    A --> B
    B --> C
    C --> E & F & G & H & I & J & K & L
    E & F & G & H & I & J & K & L --> D
    D --> A
    
    M --> N
    N --> O
    O --> E & F & G & H & I & J & K & L
```

## Shared Context Memory

```mermaid
graph TB
    subgraph "Context Management"
        A[User Context]
        B[Chat Context]
        C[Event Context]
        D[Entity Memory]
    end
    
    subgraph "Memory Types"
        E[Conversation History]
        F[User Preferences]
        G[Event Details]
        H[Domain States]
    end
    
    subgraph "Persistence"
        I[MongoDB Storage]
        J[In-Memory Cache]
        K[Context Cleanup]
    end
    
    A --> E
    B --> F
    C --> G
    D --> H
    
    E --> I
    F --> I
    G --> I
    H --> I
    
    I --> J
    J --> K
```

## Observability & Monitoring

```mermaid
graph TB
    subgraph "Flow Monitoring"
        A[Node Execution Times]
        B[Success/Failure Rates]
        C[Error Tracking]
        D[Performance Metrics]
    end
    
    subgraph "Agent Monitoring"
        E[Agent Health]
        F[Communication Stats]
        G[Tool Usage]
        H[Response Quality]
    end
    
    subgraph "System Monitoring"
        I[Memory Usage]
        J[CPU Utilization]
        K[Network Latency]
        L[Database Performance]
    end
    
    subgraph "External Monitoring"
        M[LangSmith Tracing]
        N[Grafana Dashboards]
        O[Alert System]
        P[Log Aggregation]
    end
    
    A --> M
    B --> M
    C --> M
    D --> M
    
    E --> N
    F --> N
    G --> N
    H --> N
    
    I --> O
    J --> O
    K --> O
    L --> O
    
    M --> P
    N --> P
    O --> P
```

## Data Flow Architecture

```mermaid
flowchart TD
    A[User Input] --> B[FastAPI Server]
    B --> C[LangGraph Flow Manager]
    C --> D[Routing Node]
    D --> E{Domain Analysis}
    E -->|Single Domain| F[Domain Agent]
    E -->|Multi Domain| G[Domain Agents]
    F --> H[RAG Context Retrieval]
    G --> H
    H --> I[Tool Execution]
    I --> J[Response Generation]
    J --> K[Aggregator Node]
    K --> L[Context Update]
    L --> M[Response to User]
    
    subgraph "Parallel Processing"
        N[Event Agent]
        O[Budget Agent]
        P[Venue Agent]
        Q[Vendor Agent]
    end
    
    G --> N
    G --> O
    G --> P
    G --> Q
```

## Error Handling & Resilience

```mermaid
graph TB
    subgraph "Error Detection"
        A[Node Failures]
        B[Agent Timeouts]
        C[Tool Errors]
        D[Network Issues]
    end
    
    subgraph "Recovery Strategies"
        E[Retry Logic]
        F[Fallback Agents]
        G[Circuit Breakers]
        H[Graceful Degradation]
    end
    
    subgraph "Monitoring & Alerting"
        I[Error Logging]
        J[Performance Alerts]
        K[Health Checks]
        L[Automated Recovery]
    end
    
    A --> E
    B --> F
    C --> G
    D --> H
    
    E --> I
    F --> J
    G --> K
    H --> L
```

## Security Architecture

```mermaid
graph TB
    subgraph "Authentication"
        A[API Keys]
        B[User Authentication]
        C[Agent Authorization]
        D[Session Management]
    end
    
    subgraph "Data Protection"
        E[Input Validation]
        F[Output Sanitization]
        G[Data Encryption]
        H[Access Control]
    end
    
    subgraph "Audit & Compliance"
        I[Request Logging]
        J[Agent Activity Logs]
        K[Data Access Logs]
        L[Compliance Reporting]
    end
    
    A --> E
    B --> F
    C --> G
    D --> H
    
    E --> I
    F --> J
    G --> K
    H --> L
```

## Performance Optimization

```mermaid
graph TB
    subgraph "Caching Strategy"
        A[Response Caching]
        B[Context Caching]
        C[Vector Cache]
        D[Tool Result Cache]
    end
    
    subgraph "Parallel Processing"
        E[Agent Parallelization]
        F[Tool Parallelization]
        G[Vector Search Parallelization]
        H[Response Aggregation]
    end
    
    subgraph "Resource Management"
        I[Connection Pooling]
        J[Memory Management]
        K[CPU Optimization]
        L[Network Optimization]
    end
    
    A --> E
    B --> F
    C --> G
    D --> H
    
    E --> I
    F --> J
    G --> K
    H --> L
```
