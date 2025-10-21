# Scalable Multi-Agent Architecture

## Overview

This document describes the new scalable multi-agent architecture for the Event Planner system. The architecture is designed to support easy scaling to multiple agents while maintaining clean separation of concerns and avoiding code duplication.

## Architecture Components

### 1. Core Infrastructure (`/core/`)

#### Configuration Management
- **`config/agent_config.py`**: Manages agent configurations, capabilities, and dependencies
- **`config/system_config.py`**: Manages system-wide configuration and environment variables

#### Registry System
- **`registry/agent_registry.py`**: Manages agent instances, health monitoring, and metrics
- **`registry/tool_registry.py`**: Manages tool instances, execution, and usage tracking

#### Factory System
- **`factory/agent_factory.py`**: Creates and manages agent instances with dependency injection
- **`factory/tool_factory.py`**: Creates and manages tool instances with agent-specific contexts

### 2. Agent Infrastructure (`/agents/`)

#### Core Components
- **`core/base_agent.py`**: Base class for all agents with common functionality
- **`core/base_rag.py`**: Base RAG system for knowledge management
- **`shared/`**: Shared communication and message handling components

#### Agent-Specific Directories
Each agent type has its own directory structure:
```
agents/
├── event/                    # Event planning agents
│   ├── __init__.py
│   ├── enhanced_event_agent.py
│   ├── tools/
│   │   ├── __init__.py
│   │   ├── enhanced_event_tools.py
│   │   └── validation.py
│   └── rag/
│       ├── __init__.py
│       └── event_rag.py
├── budget/                  # Budget management agents
├── venue/                   # Venue management agents
├── vendor/                  # Vendor management agents
├── attendee/                # Attendee management agents
├── communication/           # Communication agents
├── risk/                    # Risk management agents
└── weather/                 # Weather agents
```

### 3. Shared Components (`/tools/shared/`)

#### Common Tools
- **`validation.py`**: Comprehensive event validation with predictive warnings
- **`analytics.py`**: Interaction analytics and learning optimization
- **`event_insights.py`**: Proactive recommendations and insights

### 4. Agent Templates (`/agents/templates/`)

#### Template System
- **`agent_template.py`**: Template for creating new agents quickly
- **`tool_template.py`**: Template for creating agent-specific tools
- **`rag_template.py`**: Template for creating agent-specific RAG systems

## Key Features

### 1. Scalable Agent Management

#### Agent Registration
```python
from core.registry.agent_registry import AgentRegistry
from core.config.agent_config import AgentConfig, AgentType

# Create agent registry
registry = AgentRegistry()

# Register agent class
registry.register_agent_class("event_agent", EventAgent)

# Create agent instance
config = AgentConfig(
    agent_id="event_agent",
    agent_type=AgentType.EVENT,
    name="Event Planning Agent",
    description="Handles event creation and planning"
)
agent = await registry.create_agent("event_agent", config)
```

#### Agent Factory
```python
from core.factory.agent_factory import AgentFactory

# Create agent factory
factory = AgentFactory(registry)

# Create agent suite
configs = [event_config, budget_config, venue_config]
agents = await factory.create_agent_suite(configs)

# Scale agents by type
scaled_count = await factory.scale_agents_by_type(AgentType.EVENT, 5)
```

### 2. Tool Management

#### Tool Registration
```python
from core.registry.tool_registry import ToolRegistry

# Create tool registry
tool_registry = ToolRegistry()

# Register tool function
async def create_event_tool(event_data: dict) -> dict:
    return {"success": True, "event_id": "123"}

tool_registry.register_tool_function("create_event", create_event_tool)

# Execute tool
result = await tool_registry.execute_tool("create_event", {"name": "Test Event"})
```

#### Tool Factory
```python
from core.factory.tool_factory import ToolFactory

# Create tool factory
tool_factory = ToolFactory(tool_registry)

# Create tool suite
tool_ids = ["create_event", "update_event", "get_event"]
tools = await tool_factory.create_tool_suite(tool_ids)

# Create agent-specific tools
agent_tools = await tool_factory.create_agent_tools("event_agent", tool_ids)
```

### 3. Communication System

#### Message Bus
```python
from agents.shared.message_bus import MessageBus
from agents.shared.communication import CommunicationProtocol

# Create message bus
message_bus = MessageBus()
await message_bus.start()

# Create communication protocol
protocol = CommunicationProtocol("event_agent", message_bus)
await protocol.start()

# Send request
response = await protocol.send_request(
    "budget_agent",
    {"request": "calculate_budget", "event_id": "123"}
)

# Broadcast notification
await protocol.broadcast_notification(
    {"event_updated": True, "event_id": "123"}
)
```

### 4. Analytics and Learning

#### Interaction Analytics
```python
from tools.shared.analytics import InteractionAnalytics

# Create analytics system
analytics = InteractionAnalytics()

# Track interaction
await analytics.track_interaction(
    agent_id="event_agent",
    session_id="session_123",
    user_id="user_456",
    chat_id="chat_789",
    response_time_ms=150.0,
    tool_success=True,
    user_satisfaction=0.9
)

# Extract learning patterns
pattern_id = await analytics.extract_learning_patterns(
    event_type="WEDDING",
    planning_phase="CONCEPTUALIZATION",
    user_actions=["create_event", "set_budget"],
    agent_responses=["Event created", "Budget set"],
    success_metrics={"completion_rate": 0.9}
)
```

### 5. Agent Templates

#### Creating New Agents
```python
from agents.templates.agent_template import AgentTemplate

# Create agent template
template = AgentTemplate(
    agent_type="budget",
    agent_name="Budget Agent",
    description="Handles budget planning and tracking"
)

# Add tools
template.add_tool("create_budget", "create_budget", "Create a new budget")
template.add_tool("track_expense", "track_expense", "Track an expense")

# Add capabilities
template.add_capability("Budget calculation")
template.add_capability("Expense tracking")

# Set RAG system
template.set_rag_system("BudgetRAGSystem")

# Create agent directory structure
template.create_agent_directory("/path/to/agents/")
```

## Usage Examples

### 1. Creating a New Agent

```python
# 1. Create agent template
template = AgentTemplate(
    agent_type="vendor",
    agent_name="Vendor Agent",
    description="Handles vendor management and coordination"
)

# 2. Add tools and capabilities
template.add_tool("search_vendors", "search_vendors", "Search for vendors")
template.add_tool("get_vendor_quote", "get_vendor_quote", "Get vendor quote")
template.add_capability("Vendor search")
template.add_capability("Quote management")

# 3. Generate code and create directory
template.create_agent_directory("./agents/")

# 4. Register agent in system
registry.register_agent_class("vendor_agent", VendorAgent)
config = AgentConfig(
    agent_id="vendor_agent",
    agent_type=AgentType.VENDOR,
    name="Vendor Agent",
    description="Handles vendor management"
)
agent = await registry.create_agent("vendor_agent", config)
```

### 2. Scaling Agents

```python
# Scale event agents to 5 instances
scaled_count = await factory.scale_agents_by_type(AgentType.EVENT, 5)

# Scale specific tool to 3 instances
scaled_tools = await tool_factory.scale_tools("create_event", 3)

# Get system health
health = await registry.get_system_health()
print(f"Total agents: {health['total_agents']}")
print(f"Active agents: {health['active_agents']}")
print(f"System healthy: {health['healthy']}")
```

### 3. Agent Coordination

```python
# Coordinate multiple agents
coordination_result = await protocol.coordinate_with_agents(
    agents=["budget_agent", "venue_agent", "vendor_agent"],
    coordination_content={
        "task": "plan_wedding",
        "budget": 50000,
        "guest_count": 150
    }
)

# Query specific agent
budget_info = await protocol.query_agent(
    "budget_agent",
    {"query": "get_budget_status", "event_id": "123"}
)
```

## Benefits

### 1. Scalability
- Easy to add new agent types
- Independent scaling of agents and tools
- Clear separation of concerns

### 2. Maintainability
- Modular architecture
- Clear ownership of components
- Simplified testing and debugging

### 3. Flexibility
- Dynamic agent creation and removal
- Runtime configuration changes
- Easy integration of new capabilities

### 4. Performance
- Efficient resource management
- Health monitoring and recovery
- Analytics and optimization

## Testing

Run the comprehensive test suite:

```bash
python test_scalable_architecture.py
```

The test suite covers:
- Agent configuration and management
- Tool registration and execution
- Communication protocols
- Analytics and learning
- Agent templates
- System health monitoring
- Error handling and recovery
- Cleanup operations

## Migration Guide

### From Old Architecture

1. **Update imports**: Change imports to use new structure
2. **Migrate agents**: Move agents to new directory structure
3. **Update tools**: Move tools to agent-specific directories
4. **Update RAG systems**: Move RAG systems to agent-specific directories
5. **Update communication**: Use new communication protocols

### Example Migration

```python
# Old import
from agents.enhanced_event_agent import EnhancedEventAgent

# New import
from agents.event.enhanced_event_agent import EnhancedEventAgent

# Old tool import
from tools.enhanced_event_tools import start_event_creation

# New tool import
from agents.event.tools.enhanced_event_tools import start_event_creation
```

## Future Enhancements

1. **Distributed Architecture**: Support for distributed agent deployment
2. **Advanced Learning**: Machine learning-based agent optimization
3. **Dynamic Routing**: Intelligent request routing based on agent capabilities
4. **Performance Optimization**: Advanced caching and optimization strategies
5. **Monitoring Dashboard**: Real-time system monitoring and management

## Conclusion

The new scalable architecture provides a solid foundation for building and managing multiple agents in the Event Planner system. It supports easy scaling, maintenance, and extension while maintaining clean separation of concerns and avoiding code duplication.

The architecture is designed to be:
- **Scalable**: Easy to add new agents and capabilities
- **Maintainable**: Clear structure and separation of concerns
- **Flexible**: Dynamic configuration and runtime changes
- **Performant**: Efficient resource management and optimization
- **Testable**: Comprehensive testing and monitoring capabilities
