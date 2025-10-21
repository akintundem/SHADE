"""Factory for creating agent instances."""

from typing import Dict, Any, List, Optional, Type
import asyncio
import logging
from datetime import datetime

from ..config.agent_config import AgentConfig, AgentType
from ..registry.agent_registry import AgentRegistry

logger = logging.getLogger(__name__)


class AgentFactory:
    """Factory for creating and managing agent instances."""
    
    def __init__(self, agent_registry: AgentRegistry):
        """Initialize the agent factory."""
        self.agent_registry = agent_registry
        self.agent_instances: Dict[str, Any] = {}
        self._lock = asyncio.Lock()
    
    async def create_agent(self, agent_id: str, config: AgentConfig, **kwargs) -> Any:
        """Create an agent instance."""
        async with self._lock:
            if agent_id in self.agent_instances:
                logger.warning(f"Agent {agent_id} already exists, returning existing instance")
                return self.agent_instances[agent_id]
            
            try:
                # Create agent using registry
                agent = await self.agent_registry.create_agent(agent_id, config, **kwargs)
                
                # Store in factory instances
                self.agent_instances[agent_id] = agent
                
                logger.info(f"Created agent instance: {agent_id}")
                return agent
                
            except Exception as e:
                logger.error(f"Error creating agent {agent_id}: {e}")
                raise
    
    async def get_agent(self, agent_id: str) -> Optional[Any]:
        """Get an agent instance."""
        return self.agent_instances.get(agent_id)
    
    async def get_agents_by_type(self, agent_type: AgentType) -> List[Any]:
        """Get agents by type."""
        agents = []
        for agent_id, agent in self.agent_instances.items():
            if hasattr(agent, 'config') and agent.config.agent_type == agent_type:
                agents.append(agent)
        return agents
    
    async def get_all_agents(self) -> Dict[str, Any]:
        """Get all agent instances."""
        return self.agent_instances.copy()
    
    async def remove_agent(self, agent_id: str) -> bool:
        """Remove an agent instance."""
        async with self._lock:
            if agent_id in self.agent_instances:
                # Remove from registry
                await self.agent_registry.remove_agent(agent_id)
                
                # Remove from factory
                del self.agent_instances[agent_id]
                
                logger.info(f"Removed agent instance: {agent_id}")
                return True
            return False
    
    async def create_agent_suite(self, agent_configs: List[AgentConfig]) -> Dict[str, Any]:
        """Create a suite of agents."""
        agents = {}
        
        for config in agent_configs:
            try:
                agent = await self.create_agent(config.agent_id, config)
                agents[config.agent_id] = agent
                logger.info(f"Created agent in suite: {config.agent_id}")
            except Exception as e:
                logger.error(f"Error creating agent {config.agent_id} in suite: {e}")
                # Continue with other agents
        
        return agents
    
    async def create_agents_by_type(self, agent_type: AgentType, configs: List[AgentConfig]) -> List[Any]:
        """Create multiple agents of the same type."""
        agents = []
        
        for config in configs:
            if config.agent_type == agent_type:
                try:
                    agent = await self.create_agent(config.agent_id, config)
                    agents.append(agent)
                    logger.info(f"Created {agent_type.value} agent: {config.agent_id}")
                except Exception as e:
                    logger.error(f"Error creating {agent_type.value} agent {config.agent_id}: {e}")
        
        return agents
    
    async def initialize_agent_dependencies(self, agent_id: str) -> bool:
        """Initialize dependencies for an agent."""
        try:
            agent = await self.get_agent(agent_id)
            if not agent:
                return False
            
            # Get agent dependencies
            dependencies = agent.config.dependencies if hasattr(agent, 'config') else []
            
            # Initialize each dependency
            for dep_id in dependencies:
                if dep_id not in self.agent_instances:
                    # Try to create dependency agent
                    dep_config = self.agent_registry.get_agent_class(dep_id)
                    if dep_config:
                        await self.create_agent(dep_id, dep_config)
                    else:
                        logger.warning(f"Dependency agent {dep_id} not found for {agent_id}")
            
            return True
            
        except Exception as e:
            logger.error(f"Error initializing dependencies for {agent_id}: {e}")
            return False
    
    async def get_agent_health(self, agent_id: str) -> Dict[str, Any]:
        """Get agent health information."""
        return await self.agent_registry.get_agent_health(agent_id)
    
    async def get_system_health(self) -> Dict[str, Any]:
        """Get overall system health."""
        return await self.agent_registry.get_system_health()
    
    async def cleanup_inactive_agents(self, max_inactive_minutes: int = 60) -> int:
        """Clean up inactive agents."""
        return await self.agent_registry.cleanup_inactive_agents(max_inactive_minutes)
    
    async def get_agent_metrics(self) -> Dict[str, Any]:
        """Get agent metrics."""
        return await self.agent_registry.get_agent_metrics()
    
    async def restart_agent(self, agent_id: str) -> bool:
        """Restart an agent instance."""
        try:
            # Get current agent config
            agent = await self.get_agent(agent_id)
            if not agent:
                return False
            
            config = agent.config if hasattr(agent, 'config') else None
            if not config:
                return False
            
            # Remove current agent
            await self.remove_agent(agent_id)
            
            # Create new agent with same config
            new_agent = await self.create_agent(agent_id, config)
            
            logger.info(f"Restarted agent: {agent_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error restarting agent {agent_id}: {e}")
            return False
    
    async def scale_agents_by_type(self, agent_type: AgentType, target_count: int) -> int:
        """Scale agents of a specific type to target count."""
        current_agents = await self.get_agents_by_type(agent_type)
        current_count = len(current_agents)
        
        if current_count == target_count:
            return 0
        
        if current_count < target_count:
            # Need to create more agents
            needed = target_count - current_count
            created = 0
            
            # Get agent configs for this type
            configs = self.agent_registry.get_configs_by_type(agent_type)
            
            for i in range(needed):
                if i < len(configs):
                    config = configs[i]
                    try:
                        # Create unique agent ID
                        agent_id = f"{config.agent_id}_{i+1}"
                        agent = await self.create_agent(agent_id, config)
                        created += 1
                    except Exception as e:
                        logger.error(f"Error creating scaled agent {agent_id}: {e}")
            
            logger.info(f"Scaled {agent_type.value} agents: {current_count} -> {current_count + created}")
            return created
        
        else:
            # Need to remove agents
            excess = current_count - target_count
            removed = 0
            
            # Remove excess agents (keep the first ones)
            for i in range(excess):
                if i < len(current_agents):
                    agent_id = current_agents[i].config.agent_id if hasattr(current_agents[i], 'config') else f"{agent_type.value}_{i}"
                    if await self.remove_agent(agent_id):
                        removed += 1
            
            logger.info(f"Scaled down {agent_type.value} agents: {current_count} -> {current_count - removed}")
            return -removed
