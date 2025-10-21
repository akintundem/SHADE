"""Agent configuration management."""

from typing import Dict, Any, List, Optional
from dataclasses import dataclass
from enum import Enum


class AgentType(Enum):
    """Types of agents in the system."""
    EVENT = "event"
    BUDGET = "budget"
    VENUE = "venue"
    VENDOR = "vendor"
    ATTENDEE = "attendee"
    COMMUNICATION = "communication"
    RISK = "risk"
    WEATHER = "weather"
    TIMELINE = "timeline"
    PAYMENT = "payment"


class AgentCapability(Enum):
    """Agent capabilities."""
    TOOL_EXECUTION = "tool_execution"
    RAG_QUERY = "rag_query"
    WORKFLOW_COORDINATION = "workflow_coordination"
    VALIDATION = "validation"
    LEARNING = "learning"
    COMMUNICATION = "communication"


@dataclass
class AgentConfig:
    """Configuration for an agent."""
    agent_id: str
    agent_type: AgentType
    name: str
    description: str
    model_name: str = "gpt-4o"
    temperature: float = 0.7
    capabilities: List[AgentCapability] = None
    tools: List[str] = None
    rag_system: Optional[str] = None
    dependencies: List[str] = None
    max_conversation_length: int = 10
    timeout_seconds: int = 300
    retry_attempts: int = 3
    priority: int = 1  # 1 = highest, 5 = lowest
    enabled: bool = True
    
    def __post_init__(self):
        if self.capabilities is None:
            self.capabilities = [AgentCapability.TOOL_EXECUTION]
        if self.tools is None:
            self.tools = []
        if self.dependencies is None:
            self.dependencies = []


class AgentConfigManager:
    """Manages agent configurations."""
    
    def __init__(self):
        """Initialize the agent config manager."""
        self.configs: Dict[str, AgentConfig] = {}
        self._load_default_configs()
    
    def _load_default_configs(self):
        """Load default agent configurations."""
        # Event Agent Configuration
        self.configs["event"] = AgentConfig(
            agent_id="event",
            agent_type=AgentType.EVENT,
            name="Event Planning Agent",
            description="Handles event creation, modification, and planning",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY,
                AgentCapability.WORKFLOW_COORDINATION,
                AgentCapability.VALIDATION,
                AgentCapability.LEARNING
            ],
            tools=[
                "start_event_creation",
                "enhance_event_details", 
                "get_event_info",
                "get_current_event_status",
                "check_event_weather",
                "search_venues_google"
            ],
            rag_system="event_rag",
            priority=1
        )
        
        # Budget Agent Configuration
        self.configs["budget"] = AgentConfig(
            agent_id="budget",
            agent_type=AgentType.BUDGET,
            name="Budget Management Agent",
            description="Handles budget planning, tracking, and optimization",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY,
                AgentCapability.VALIDATION
            ],
            tools=[
                "create_budget",
                "add_budget_item",
                "calculate_budget",
                "track_payment"
            ],
            rag_system="budget_rag",
            priority=2
        )
        
        # Venue Agent Configuration
        self.configs["venue"] = AgentConfig(
            agent_id="venue",
            agent_type=AgentType.VENUE,
            name="Venue Management Agent",
            description="Handles venue search, selection, and booking",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY,
                AgentCapability.VALIDATION
            ],
            tools=[
                "search_venues",
                "get_venue_details",
                "book_venue"
            ],
            rag_system="venue_rag",
            priority=2
        )
        
        # Vendor Agent Configuration
        self.configs["vendor"] = AgentConfig(
            agent_id="vendor",
            agent_type=AgentType.VENDOR,
            name="Vendor Management Agent",
            description="Handles vendor search, selection, and coordination",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY,
                AgentCapability.VALIDATION
            ],
            tools=[
                "search_vendors",
                "get_vendor_quote",
                "book_vendor",
                "list_vendor_services"
            ],
            rag_system="vendor_rag",
            priority=2
        )
        
        # Attendee Agent Configuration
        self.configs["attendee"] = AgentConfig(
            agent_id="attendee",
            agent_type=AgentType.ATTENDEE,
            name="Attendee Management Agent",
            description="Handles attendee management, invitations, and RSVPs",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY,
                AgentCapability.COMMUNICATION
            ],
            tools=[
                "add_attendee",
                "send_invitations",
                "track_rsvps",
                "manage_guest_list"
            ],
            rag_system="attendee_rag",
            priority=3
        )
        
        # Communication Agent Configuration
        self.configs["communication"] = AgentConfig(
            agent_id="communication",
            agent_type=AgentType.COMMUNICATION,
            name="Communication Agent",
            description="Handles all communication and notifications",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY,
                AgentCapability.COMMUNICATION
            ],
            tools=[
                "send_notification",
                "generate_report"
            ],
            rag_system="communication_rag",
            priority=3
        )
        
        # Risk Agent Configuration
        self.configs["risk"] = AgentConfig(
            agent_id="risk",
            agent_type=AgentType.RISK,
            name="Risk Management Agent",
            description="Handles risk assessment and mitigation",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY,
                AgentCapability.VALIDATION
            ],
            tools=[
                "assess_risks",
                "check_weather"
            ],
            rag_system="risk_rag",
            priority=2
        )
        
        # Weather Agent Configuration
        self.configs["weather"] = AgentConfig(
            agent_id="weather",
            agent_type=AgentType.WEATHER,
            name="Weather Agent",
            description="Handles weather monitoring and forecasting",
            capabilities=[
                AgentCapability.TOOL_EXECUTION,
                AgentCapability.RAG_QUERY
            ],
            tools=[
                "check_weather",
                "get_weather_forecast"
            ],
            rag_system="weather_rag",
            priority=4
        )
    
    def get_config(self, agent_id: str) -> Optional[AgentConfig]:
        """Get configuration for an agent."""
        return self.configs.get(agent_id)
    
    def get_all_configs(self) -> Dict[str, AgentConfig]:
        """Get all agent configurations."""
        return self.configs.copy()
    
    def get_configs_by_type(self, agent_type: AgentType) -> List[AgentConfig]:
        """Get configurations by agent type."""
        return [config for config in self.configs.values() if config.agent_type == agent_type]
    
    def get_configs_by_capability(self, capability: AgentCapability) -> List[AgentConfig]:
        """Get configurations by capability."""
        return [config for config in self.configs.values() if capability in config.capabilities]
    
    def add_config(self, config: AgentConfig) -> None:
        """Add a new agent configuration."""
        self.configs[config.agent_id] = config
    
    def update_config(self, agent_id: str, updates: Dict[str, Any]) -> None:
        """Update an agent configuration."""
        if agent_id in self.configs:
            config = self.configs[agent_id]
            for key, value in updates.items():
                if hasattr(config, key):
                    setattr(config, key, value)
    
    def remove_config(self, agent_id: str) -> bool:
        """Remove an agent configuration."""
        if agent_id in self.configs:
            del self.configs[agent_id]
            return True
        return False
    
    def get_enabled_agents(self) -> List[AgentConfig]:
        """Get all enabled agent configurations."""
        return [config for config in self.configs.values() if config.enabled]
    
    def get_agent_dependencies(self, agent_id: str) -> List[str]:
        """Get dependencies for an agent."""
        config = self.configs.get(agent_id)
        if config:
            return config.dependencies
        return []
