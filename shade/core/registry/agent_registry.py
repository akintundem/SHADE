"""Agent registry for managing agent instances."""

from typing import Dict, Any, List, Optional, Type
import asyncio
import logging
from datetime import datetime

from ..config.agent_config import AgentConfig, AgentType

logger = logging.getLogger(__name__)


class AgentRegistry:
    """Registry for managing agent instances."""
    
    def __init__(self):
        """Initialize the agent registry."""
        self.agents: Dict[str, Any] = {}
        self.agent_classes: Dict[str, Type] = {}
        self.agent_status: Dict[str, Dict[str, Any]] = {}
        self._lock = asyncio.Lock()
    
    def register_agent_class(self, agent_id: str, agent_class: Type) -> None:
        """Register an agent class."""
        self.agent_classes[agent_id] = agent_class
        logger.info(f"Registered agent class: {agent_id}")
    
    def get_agent_class(self, agent_id: str) -> Optional[Type]:
        """Get an agent class by ID."""
        return self.agent_classes.get(agent_id)
    
    async def create_agent(self, agent_id: str, config: AgentConfig, **kwargs) -> Any:
        """Create an agent instance."""
        async with self._lock:
            if agent_id in self.agents:
                logger.warning(f"Agent {agent_id} already exists, returning existing instance")
                return self.agents[agent_id]
            
            agent_class = self.agent_classes.get(agent_id)
            if not agent_class:
                raise ValueError(f"Agent class not found for {agent_id}")
            
            try:
                # Create agent instance
                agent = agent_class(config=config, **kwargs)
                
                # Initialize agent
                if hasattr(agent, 'setup_agent'):
                    await agent.setup_agent()
                
                # Store agent
                self.agents[agent_id] = agent
                self.agent_status[agent_id] = {
                    "status": "active",
                    "created_at": datetime.utcnow().isoformat(),
                    "last_activity": datetime.utcnow().isoformat(),
                    "request_count": 0,
                    "error_count": 0
                }
                
                logger.info(f"Created agent: {agent_id}")
                return agent
                
            except Exception as e:
                logger.error(f"Error creating agent {agent_id}: {e}")
                raise
    
    async def get_agent(self, agent_id: str) -> Optional[Any]:
        """Get an agent instance."""
        return self.agents.get(agent_id)
    
    async def get_agents_by_type(self, agent_type: AgentType) -> List[Any]:
        """Get agents by type."""
        agents = []
        for agent_id, agent in self.agents.items():
            if hasattr(agent, 'config') and agent.config.agent_type == agent_type:
                agents.append(agent)
        return agents
    
    async def get_all_agents(self) -> Dict[str, Any]:
        """Get all agent instances."""
        return self.agents.copy()
    
    async def remove_agent(self, agent_id: str) -> bool:
        """Remove an agent instance."""
        async with self._lock:
            if agent_id in self.agents:
                # Cleanup agent if needed
                agent = self.agents[agent_id]
                if hasattr(agent, 'cleanup'):
                    try:
                        await agent.cleanup()
                    except Exception as e:
                        logger.error(f"Error cleaning up agent {agent_id}: {e}")
                
                del self.agents[agent_id]
                if agent_id in self.agent_status:
                    del self.agent_status[agent_id]
                
                logger.info(f"Removed agent: {agent_id}")
                return True
            return False
    
    async def update_agent_status(self, agent_id: str, status: str) -> None:
        """Update agent status."""
        if agent_id in self.agent_status:
            self.agent_status[agent_id]["status"] = status
            self.agent_status[agent_id]["last_activity"] = datetime.utcnow().isoformat()
    
    async def increment_request_count(self, agent_id: str) -> None:
        """Increment request count for an agent."""
        if agent_id in self.agent_status:
            self.agent_status[agent_id]["request_count"] += 1
            self.agent_status[agent_id]["last_activity"] = datetime.utcnow().isoformat()
    
    async def increment_error_count(self, agent_id: str) -> None:
        """Increment error count for an agent."""
        if agent_id in self.agent_status:
            self.agent_status[agent_id]["error_count"] += 1
    
    async def get_agent_status(self, agent_id: str) -> Optional[Dict[str, Any]]:
        """Get agent status."""
        return self.agent_status.get(agent_id)
    
    async def get_all_agent_status(self) -> Dict[str, Dict[str, Any]]:
        """Get status of all agents."""
        return self.agent_status.copy()
    
    async def get_agent_health(self, agent_id: str) -> Dict[str, Any]:
        """Get agent health information."""
        if agent_id not in self.agent_status:
            return {"status": "not_found", "healthy": False}
        
        status = self.agent_status[agent_id]
        error_rate = 0
        if status["request_count"] > 0:
            error_rate = status["error_count"] / status["request_count"]
        
        return {
            "status": status["status"],
            "healthy": status["status"] == "active" and error_rate < 0.1,
            "request_count": status["request_count"],
            "error_count": status["error_count"],
            "error_rate": error_rate,
            "last_activity": status["last_activity"],
            "created_at": status["created_at"]
        }
    
    async def get_system_health(self) -> Dict[str, Any]:
        """Get overall system health."""
        total_agents = len(self.agents)
        active_agents = sum(1 for status in self.agent_status.values() if status["status"] == "active")
        
        total_requests = sum(status["request_count"] for status in self.agent_status.values())
        total_errors = sum(status["error_count"] for status in self.agent_status.values())
        
        overall_error_rate = 0
        if total_requests > 0:
            overall_error_rate = total_errors / total_requests
        
        return {
            "total_agents": total_agents,
            "active_agents": active_agents,
            "total_requests": total_requests,
            "total_errors": total_errors,
            "overall_error_rate": overall_error_rate,
            "healthy": active_agents == total_agents and overall_error_rate < 0.1
        }
    
    async def cleanup_inactive_agents(self, max_inactive_minutes: int = 60) -> int:
        """Clean up inactive agents."""
        cutoff_time = datetime.utcnow().timestamp() - (max_inactive_minutes * 60)
        cleaned_count = 0
        
        async with self._lock:
            agents_to_remove = []
            for agent_id, status in self.agent_status.items():
                last_activity = datetime.fromisoformat(status["last_activity"]).timestamp()
                if last_activity < cutoff_time and status["status"] != "active":
                    agents_to_remove.append(agent_id)
            
            for agent_id in agents_to_remove:
                await self.remove_agent(agent_id)
                cleaned_count += 1
        
        return cleaned_count
    
    async def get_agent_metrics(self) -> Dict[str, Any]:
        """Get agent metrics."""
        metrics = {
            "total_agents": len(self.agents),
            "agent_types": {},
            "capabilities": {},
            "performance": {}
        }
        
        for agent_id, agent in self.agents.items():
            if hasattr(agent, 'config'):
                # Count by type
                agent_type = agent.config.agent_type.value
                metrics["agent_types"][agent_type] = metrics["agent_types"].get(agent_type, 0) + 1
                
                # Count by capability
                for capability in agent.config.capabilities:
                    cap_name = capability.value
                    metrics["capabilities"][cap_name] = metrics["capabilities"].get(cap_name, 0) + 1
        
        # Add performance metrics
        for agent_id, status in self.agent_status.items():
            metrics["performance"][agent_id] = {
                "request_count": status["request_count"],
                "error_count": status["error_count"],
                "error_rate": status["error_count"] / max(status["request_count"], 1)
            }
        
        return metrics
