flowchart LR
  %% Entry
  C[Web/Mobile Clients]
  K[Kong API Gateway]

  %% Internal services (Docker network)
  subgraph N["Docker network: event-planner-network"]
    subgraph EP_APP["Event Planner Monolith (Spring Boot)"]
      EP_API[REST API]
      EP_AUTH[Auth & RBAC]
      EP_EVENTS[Events]
      EP_TICKETS[Tickets]
      EP_BUDGET[Budget]
      EP_TIMELINE[Timeline]
      EP_ATTENDEES[Attendees]
      EP_FEEDS[Feeds]
    end
    AI["AI Service<br/>Python"]
    ES["Email Service<br/>Node"]
    PS["Push Service<br/>Node"]
  end

  %% External systems
  subgraph X["External systems"]
    PG[(PostgreSQL)]
    RD[(Redis)]
    S3[("S3-Compatible Storage")]
    CG[("Cognito")]
  end

  C -->|Sign-in| CG
  CG -->|JWT access token| C
  C -->|Bearer JWT| K
  K -->|/api| EP_API
  K -->|/ai-service| AI

  EP_API --> EP_AUTH
  EP_API --> EP_EVENTS
  EP_API --> EP_TICKETS
  EP_API --> EP_BUDGET
  EP_API --> EP_TIMELINE
  EP_API --> EP_ATTENDEES
  EP_API --> EP_FEEDS

  EP_API --> ES
  EP_API --> PS
  EP_API --> PG
  EP_API --> RD
  EP_API --> S3
  EP_AUTH -->|JWT validation| CG

  AI -->|JWT validation| CG
