"""Factory for creating tool instances."""

from typing import Dict, Any, List, Optional, Type, Callable
import asyncio
import logging
from datetime import datetime

from ..registry.tool_registry import ToolRegistry

logger = logging.getLogger(__name__)


class ToolFactory:
    """Factory for creating and managing tool instances."""
    
    def __init__(self, tool_registry: ToolRegistry):
        """Initialize the tool factory."""
        self.tool_registry = tool_registry
        self.tool_instances: Dict[str, Any] = {}
        self._lock = asyncio.Lock()
    
    async def create_tool(self, tool_id: str, **kwargs) -> Any:
        """Create a tool instance."""
        async with self._lock:
            if tool_id in self.tool_instances:
                logger.warning(f"Tool {tool_id} already exists, returning existing instance")
                return self.tool_instances[tool_id]
            
            try:
                # Create tool using registry
                tool = await self.tool_registry.create_tool(tool_id, **kwargs)
                
                # Store in factory instances
                self.tool_instances[tool_id] = tool
                
                logger.info(f"Created tool instance: {tool_id}")
                return tool
                
            except Exception as e:
                logger.error(f"Error creating tool {tool_id}: {e}")
                raise
    
    async def get_tool(self, tool_id: str) -> Optional[Any]:
        """Get a tool instance."""
        return self.tool_instances.get(tool_id)
    
    async def get_all_tools(self) -> Dict[str, Any]:
        """Get all tool instances."""
        return self.tool_instances.copy()
    
    async def remove_tool(self, tool_id: str) -> bool:
        """Remove a tool instance."""
        async with self._lock:
            if tool_id in self.tool_instances:
                # Remove from registry
                await self.tool_registry.remove_tool(tool_id)
                
                # Remove from factory
                del self.tool_instances[tool_id]
                
                logger.info(f"Removed tool instance: {tool_id}")
                return True
            return False
    
    async def create_tool_suite(self, tool_ids: List[str]) -> Dict[str, Any]:
        """Create a suite of tools."""
        tools = {}
        
        for tool_id in tool_ids:
            try:
                tool = await self.create_tool(tool_id)
                tools[tool_id] = tool
                logger.info(f"Created tool in suite: {tool_id}")
            except Exception as e:
                logger.error(f"Error creating tool {tool_id} in suite: {e}")
                # Continue with other tools
        
        return tools
    
    async def execute_tool(self, tool_id: str, parameters: Dict[str, Any]) -> Any:
        """Execute a tool."""
        try:
            return await self.tool_registry.execute_tool(tool_id, parameters)
        except Exception as e:
            logger.error(f"Error executing tool {tool_id}: {e}")
            raise
    
    async def get_tool_health(self, tool_id: str) -> Dict[str, Any]:
        """Get tool health information."""
        return await self.tool_registry.get_tool_health(tool_id)
    
    async def get_system_health(self) -> Dict[str, Any]:
        """Get overall system health."""
        return await self.tool_registry.get_system_health()
    
    async def cleanup_unused_tools(self, max_unused_hours: int = 24) -> int:
        """Clean up unused tools."""
        return await self.tool_registry.cleanup_unused_tools(max_unused_hours)
    
    async def get_tool_metrics(self) -> Dict[str, Any]:
        """Get tool metrics."""
        return await self.tool_registry.get_tool_metrics()
    
    async def restart_tool(self, tool_id: str) -> bool:
        """Restart a tool instance."""
        try:
            # Get current tool
            tool = await self.get_tool(tool_id)
            if not tool:
                return False
            
            # Remove current tool
            await self.remove_tool(tool_id)
            
            # Create new tool
            new_tool = await self.create_tool(tool_id)
            
            logger.info(f"Restarted tool: {tool_id}")
            return True
            
        except Exception as e:
            logger.error(f"Error restarting tool {tool_id}: {e}")
            return False
    
    async def create_agent_tools(self, agent_id: str, tool_ids: List[str]) -> Dict[str, Any]:
        """Create tools for a specific agent."""
        tools = {}
        
        for tool_id in tool_ids:
            try:
                # Create tool with agent context
                tool = await self.create_tool(tool_id, agent_id=agent_id)
                tools[tool_id] = tool
                logger.info(f"Created tool {tool_id} for agent {agent_id}")
            except Exception as e:
                logger.error(f"Error creating tool {tool_id} for agent {agent_id}: {e}")
        
        return tools
    
    async def get_agent_tools(self, agent_id: str) -> Dict[str, Any]:
        """Get tools for a specific agent."""
        agent_tools = {}
        
        for tool_id, tool in self.tool_instances.items():
            if hasattr(tool, 'agent_id') and tool.agent_id == agent_id:
                agent_tools[tool_id] = tool
        
        return agent_tools
    
    async def remove_agent_tools(self, agent_id: str) -> int:
        """Remove all tools for a specific agent."""
        removed_count = 0
        
        for tool_id, tool in list(self.tool_instances.items()):
            if hasattr(tool, 'agent_id') and tool.agent_id == agent_id:
                if await self.remove_tool(tool_id):
                    removed_count += 1
        
        logger.info(f"Removed {removed_count} tools for agent {agent_id}")
        return removed_count
    
    async def get_tool_usage_stats(self, tool_id: str) -> Dict[str, Any]:
        """Get usage statistics for a tool."""
        return await self.tool_registry.get_tool_usage(tool_id)
    
    async def get_all_tool_usage_stats(self) -> Dict[str, Dict[str, Any]]:
        """Get usage statistics for all tools."""
        return await self.tool_registry.get_all_tool_usage()
    
    async def scale_tools(self, tool_id: str, target_count: int) -> int:
        """Scale a specific tool to target count."""
        current_tools = [tool for tool in self.tool_instances.values() 
                        if hasattr(tool, 'tool_id') and tool.tool_id == tool_id]
        current_count = len(current_tools)
        
        if current_count == target_count:
            return 0
        
        if current_count < target_count:
            # Need to create more tools
            needed = target_count - current_count
            created = 0
            
            for i in range(needed):
                try:
                    # Create unique tool ID
                    unique_tool_id = f"{tool_id}_{i+1}"
                    tool = await self.create_tool(unique_tool_id)
                    created += 1
                except Exception as e:
                    logger.error(f"Error creating scaled tool {unique_tool_id}: {e}")
            
            logger.info(f"Scaled {tool_id} tools: {current_count} -> {current_count + created}")
            return created
        
        else:
            # Need to remove tools
            excess = current_count - target_count
            removed = 0
            
            # Remove excess tools
            for i in range(excess):
                if i < len(current_tools):
                    tool_instance = current_tools[i]
                    if hasattr(tool_instance, 'tool_id'):
                        tool_id_to_remove = tool_instance.tool_id
                        if await self.remove_tool(tool_id_to_remove):
                            removed += 1
            
            logger.info(f"Scaled down {tool_id} tools: {current_count} -> {current_count - removed}")
            return -removed
