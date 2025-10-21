"""Test the new scalable multi-agent architecture."""

import pytest
import asyncio
from unittest.mock import AsyncMock, MagicMock
from datetime import datetime

# Import the new scalable components
from core.config.agent_config import AgentConfig, AgentType, AgentCapability
from core.config.system_config import SystemConfig, SystemConfigManager
from core.registry.agent_registry import AgentRegistry
from core.registry.tool_registry import ToolRegistry
from core.factory.agent_factory import AgentFactory
from core.factory.tool_factory import ToolFactory
from agents.core.base_agent import BaseAgent
from agents.event.enhanced_event_agent import EnhancedEventAgent
from agents.shared.communication import MessageBus, CommunicationProtocol
from agents.templates.agent_template import AgentTemplate
from tools.shared.analytics import InteractionAnalytics


class TestScalableArchitecture:
    """Test the scalable multi-agent architecture."""
    
    @pytest.fixture
    def system_config(self):
        """Create system configuration."""
        return SystemConfigManager()
    
    @pytest.fixture
    def agent_registry(self):
        """Create agent registry."""
        return AgentRegistry()
    
    @pytest.fixture
    def tool_registry(self):
        """Create tool registry."""
        return ToolRegistry()
    
    @pytest.fixture
    def agent_factory(self, agent_registry):
        """Create agent factory."""
        return AgentFactory(agent_registry)
    
    @pytest.fixture
    def tool_factory(self, tool_registry):
        """Create tool factory."""
        return ToolFactory(tool_registry)
    
    @pytest.fixture
    def message_bus(self):
        """Create message bus."""
        return MessageBus()
    
    @pytest.fixture
    def analytics(self):
        """Create analytics system."""
        return InteractionAnalytics()
    
    @pytest.mark.asyncio
    async def test_agent_config_management(self, system_config):
        """Test agent configuration management."""
        # Test system config
        config = system_config.get_config()
        assert config.app_name == "Event Planner AI"
        assert config.debug == False
        
        # Test database config
        db_config = system_config.get_database_config()
        assert db_config["type"] == "mongodb"
        
        # Test API keys
        api_keys = system_config.get_api_keys()
        assert "openai" in api_keys
        assert "google" in api_keys
    
    @pytest.mark.asyncio
    async def test_agent_registry_operations(self, agent_registry):
        """Test agent registry operations."""
        # Test agent class registration
        class MockAgent:
            def __init__(self, config):
                self.config = config
        
        agent_registry.register_agent_class("test_agent", MockAgent)
        assert "test_agent" in agent_registry.agent_classes
        
        # Test agent creation
        config = AgentConfig(
            agent_id="test_agent",
            agent_type=AgentType.EVENT,
            name="Test Agent",
            description="Test agent for testing"
        )
        
        agent = await agent_registry.create_agent("test_agent", config)
        assert agent is not None
        assert "test_agent" in agent_registry.agents
        
        # Test agent removal
        removed = await agent_registry.remove_agent("test_agent")
        assert removed == True
        assert "test_agent" not in agent_registry.agents
    
    @pytest.mark.asyncio
    async def test_tool_registry_operations(self, tool_registry):
        """Test tool registry operations."""
        # Test tool function registration
        async def mock_tool_function(param: str) -> dict:
            return {"result": f"Tool executed with {param}"}
        
        tool_registry.register_tool_function("test_tool", mock_tool_function)
        assert "test_tool" in tool_registry.tool_functions
        
        # Test tool execution
        result = await tool_registry.execute_tool("test_tool", {"param": "test_value"})
        assert result["result"] == "Tool executed with test_value"
        
        # Test tool removal
        removed = await tool_registry.remove_tool("test_tool")
        assert removed == True
        assert "test_tool" not in tool_registry.tool_functions
    
    @pytest.mark.asyncio
    async def test_agent_factory_operations(self, agent_factory):
        """Test agent factory operations."""
        # Test agent creation
        config = AgentConfig(
            agent_id="factory_test",
            agent_type=AgentType.EVENT,
            name="Factory Test Agent",
            description="Test agent created by factory"
        )
        
        # Register agent class first
        class MockAgent:
            def __init__(self, config):
                self.config = config
        
        agent_factory.agent_registry.register_agent_class("factory_test", MockAgent)
        
        agent = await agent_factory.create_agent("factory_test", config)
        assert agent is not None
        assert "factory_test" in agent_factory.agent_instances
        
        # Test agent suite creation
        configs = [
            AgentConfig(
                agent_id="suite_agent_1",
                agent_type=AgentType.EVENT,
                name="Suite Agent 1",
                description="First suite agent"
            ),
            AgentConfig(
                agent_id="suite_agent_2",
                agent_type=AgentType.BUDGET,
                name="Suite Agent 2",
                description="Second suite agent"
            )
        ]
        
        # Register agent classes
        agent_factory.agent_registry.register_agent_class("suite_agent_1", MockAgent)
        agent_factory.agent_registry.register_agent_class("suite_agent_2", MockAgent)
        
        agents = await agent_factory.create_agent_suite(configs)
        assert len(agents) == 2
        assert "suite_agent_1" in agents
        assert "suite_agent_2" in agents
    
    @pytest.mark.asyncio
    async def test_tool_factory_operations(self, tool_factory):
        """Test tool factory operations."""
        # Test tool creation
        async def mock_tool_function(param: str) -> dict:
            return {"result": f"Tool executed with {param}"}
        
        tool_factory.tool_registry.register_tool_function("factory_tool", mock_tool_function)
        
        tool = await tool_factory.create_tool("factory_tool")
        assert tool is not None
        assert "factory_tool" in tool_factory.tool_instances
        
        # Test tool suite creation
        tool_ids = ["tool_1", "tool_2", "tool_3"]
        
        # Register tool functions
        for tool_id in tool_ids:
            async def mock_tool(param: str = "default") -> dict:
                return {"result": f"Tool {tool_id} executed"}
            tool_factory.tool_registry.register_tool_function(tool_id, mock_tool)
        
        tools = await tool_factory.create_tool_suite(tool_ids)
        assert len(tools) == 3
        for tool_id in tool_ids:
            assert tool_id in tools
    
    @pytest.mark.asyncio
    async def test_message_bus_communication(self, message_bus):
        """Test message bus communication."""
        # Start message bus
        await message_bus.start()
        
        # Test message handling
        received_messages = []
        
        async def message_handler(message):
            received_messages.append(message)
        
        # Subscribe agent
        await message_bus.subscribe("test_agent", message_handler)
        
        # Send message
        from agents.shared.agent_message import AgentMessage, MessageType, MessagePriority
        
        message = AgentMessage(
            sender="sender_agent",
            recipient="test_agent",
            message_type=MessageType.REQUEST,
            content={"test": "data"},
            priority=MessagePriority.NORMAL
        )
        
        await message_bus.send_message(message)
        
        # Wait for message processing
        await asyncio.sleep(0.1)
        
        # Check message was received
        assert len(received_messages) == 1
        assert received_messages[0].content["test"] == "data"
        
        # Test broadcast
        await message_bus.broadcast_message(
            "broadcaster",
            MessageType.NOTIFICATION,
            {"broadcast": "message"},
            MessagePriority.NORMAL
        )
        
        await asyncio.sleep(0.1)
        
        # Should have received broadcast message
        assert len(received_messages) == 2
        
        await message_bus.stop()
    
    @pytest.mark.asyncio
    async def test_communication_protocol(self, message_bus):
        """Test communication protocol."""
        # Start message bus
        await message_bus.start()
        
        # Create communication protocol
        protocol = CommunicationProtocol("test_agent", message_bus)
        await protocol.start()
        
        # Test request-response
        response = await protocol.send_request(
            "target_agent",
            {"request": "data"},
            timeout=1.0
        )
        
        # Should timeout since no target agent
        assert response is None
        
        await protocol.stop()
        await message_bus.stop()
    
    @pytest.mark.asyncio
    async def test_analytics_tracking(self, analytics):
        """Test analytics tracking."""
        # Track interaction
        await analytics.track_interaction(
            agent_id="test_agent",
            session_id="test_session",
            user_id="test_user",
            chat_id="test_chat",
            response_time_ms=100.0,
            tool_success=True,
            user_satisfaction=0.8
        )
        
        # Get agent performance
        performance = await analytics.get_agent_performance("test_agent")
        assert performance["agent_id"] == "test_agent"
        assert performance["total_requests"] == 1
        assert performance["avg_satisfaction"] == 0.8
        
        # Test learning pattern extraction
        pattern_id = await analytics.extract_learning_patterns(
            event_type="WEDDING",
            planning_phase="CONCEPTUALIZATION",
            user_actions=["create_event", "set_budget"],
            agent_responses=["Event created", "Budget set"],
            success_metrics={"completion_rate": 0.9, "user_satisfaction": 0.8}
        )
        
        assert pattern_id is not None
        
        # Get learning patterns
        patterns = await analytics.get_learning_patterns("WEDDING", "CONCEPTUALIZATION")
        assert len(patterns) == 1
        assert patterns[0]["pattern_id"] == pattern_id
    
    @pytest.mark.asyncio
    async def test_agent_template_creation(self):
        """Test agent template creation."""
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
        template.add_capability("Financial reporting")
        
        # Set RAG system
        template.set_rag_system("BudgetRAGSystem")
        
        # Test template info
        info = template.get_template_info()
        assert info["agent_type"] == "budget"
        assert info["agent_name"] == "Budget Agent"
        assert len(info["tools"]) == 2
        assert len(info["capabilities"]) == 3
        assert info["rag_system"] == "BudgetRAGSystem"
        
        # Test code generation
        agent_code = template.generate_agent_code()
        assert "class BudgetAgent(BaseAgent):" in agent_code
        assert "create_budget" in agent_code
        assert "track_expense" in agent_code
        
        tools_code = template.generate_tools_code()
        assert "@tool" in tools_code
        assert "async def create_budget" in tools_code
        
        rag_code = template.generate_rag_code()
        assert "class BudgetRAGSystem" in rag_code
        assert "budget" in rag_code.lower()
    
    @pytest.mark.asyncio
    async def test_enhanced_event_agent_integration(self, message_bus):
        """Test enhanced event agent integration."""
        # Create enhanced event agent
        agent = EnhancedEventAgent(message_bus=message_bus)
        
        # Test agent info
        info = agent.get_agent_info()
        assert info["name"] == "Enhanced Event Agent"
        assert "tools" in info
        assert "has_rag" in info
        
        # Test system prompt
        prompt = agent.get_system_prompt()
        assert "Event Planner" in prompt
        assert "CREATE EVENT" in prompt
        assert "ENHANCE EVENT" in prompt
        
        # Test dynamic context
        context = {
            "event_type": "WEDDING",
            "learned_patterns": ["Book venue early", "Set budget first"],
            "validation_context": "Check capacity requirements",
            "workflow_context": "Planning phase"
        }
        
        dynamic_prompt = agent.get_system_prompt(context)
        assert "WEDDING PLANNING GUIDANCE" in dynamic_prompt
        assert "SUCCESSFUL PATTERNS" in dynamic_prompt
        assert "VALIDATION NOTES" in dynamic_prompt
        assert "WORKFLOW STATUS" in dynamic_prompt
    
    @pytest.mark.asyncio
    async def test_system_health_monitoring(self, agent_registry, tool_registry):
        """Test system health monitoring."""
        # Test agent health
        health = await agent_registry.get_system_health()
        assert "total_agents" in health
        assert "active_agents" in health
        assert "healthy" in health
        
        # Test tool health
        tool_health = await tool_registry.get_system_health()
        assert "total_tools" in tool_health
        assert "overall_error_rate" in tool_health
        assert "healthy" in tool_health
        
        # Test agent metrics
        metrics = await agent_registry.get_agent_metrics()
        assert "total_agents" in metrics
        assert "agent_types" in metrics
        assert "capabilities" in metrics
    
    @pytest.mark.asyncio
    async def test_scalability_features(self, agent_factory, tool_factory):
        """Test scalability features."""
        # Test agent scaling
        config = AgentConfig(
            agent_id="scalable_agent",
            agent_type=AgentType.EVENT,
            name="Scalable Agent",
            description="Test scalable agent"
        )
        
        class MockAgent:
            def __init__(self, config):
                self.config = config
        
        agent_factory.agent_registry.register_agent_class("scalable_agent", MockAgent)
        
        # Create initial agent
        agent = await agent_factory.create_agent("scalable_agent", config)
        assert agent is not None
        
        # Test scaling up
        scaled_count = await agent_factory.scale_agents_by_type(AgentType.EVENT, 3)
        assert scaled_count > 0
        
        # Test tool scaling
        async def mock_tool(param: str = "default") -> dict:
            return {"result": "Tool executed"}
        
        tool_factory.tool_registry.register_tool_function("scalable_tool", mock_tool)
        
        # Create initial tool
        tool = await tool_factory.create_tool("scalable_tool")
        assert tool is not None
        
        # Test tool scaling
        scaled_tools = await tool_factory.scale_tools("scalable_tool", 2)
        assert scaled_tools > 0
    
    @pytest.mark.asyncio
    async def test_error_handling_and_recovery(self, agent_registry, tool_registry):
        """Test error handling and recovery."""
        # Test agent restart
        config = AgentConfig(
            agent_id="restart_agent",
            agent_type=AgentType.EVENT,
            name="Restart Agent",
            description="Test restart agent"
        )
        
        class MockAgent:
            def __init__(self, config):
                self.config = config
        
        agent_registry.register_agent_class("restart_agent", MockAgent)
        
        # Create agent
        agent = await agent_registry.create_agent("restart_agent", config)
        assert agent is not None
        
        # Test agent restart
        restarted = await agent_registry.restart_agent("restart_agent")
        assert restarted == True
        
        # Test tool restart
        async def mock_tool(param: str = "default") -> dict:
            return {"result": "Tool executed"}
        
        tool_registry.register_tool_function("restart_tool", mock_tool)
        
        # Create tool
        tool = await tool_registry.create_tool("restart_tool")
        assert tool is not None
        
        # Test tool restart
        restarted = await tool_registry.restart_tool("restart_tool")
        assert restarted == True
    
    @pytest.mark.asyncio
    async def test_cleanup_operations(self, agent_registry, tool_registry):
        """Test cleanup operations."""
        # Test agent cleanup
        cleaned_agents = await agent_registry.cleanup_inactive_agents(max_inactive_minutes=0)
        assert isinstance(cleaned_agents, int)
        
        # Test tool cleanup
        cleaned_tools = await tool_registry.cleanup_unused_tools(max_unused_hours=0)
        assert isinstance(cleaned_tools, int)
        
        # Test analytics cleanup
        analytics = InteractionAnalytics()
        cleaned_metrics = await analytics.cleanup_old_metrics(days=0)
        assert isinstance(cleaned_metrics, int)


if __name__ == "__main__":
    # Run the tests
    pytest.main([__file__, "-v"])
