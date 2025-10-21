"""Tool registry for managing tool instances."""

from typing import Dict, Any, List, Optional, Type, Callable
import asyncio
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class ToolRegistry:
    """Registry for managing tool instances."""
    
    def __init__(self):
        """Initialize the tool registry."""
        self.tools: Dict[str, Any] = {}
        self.tool_classes: Dict[str, Type] = {}
        self.tool_functions: Dict[str, Callable] = {}
        self.tool_usage: Dict[str, Dict[str, Any]] = {}
        self._lock = asyncio.Lock()
    
    def register_tool_class(self, tool_id: str, tool_class: Type) -> None:
        """Register a tool class."""
        self.tool_classes[tool_id] = tool_class
        logger.info(f"Registered tool class: {tool_id}")
    
    def register_tool_function(self, tool_id: str, tool_function: Callable) -> None:
        """Register a tool function."""
        self.tool_functions[tool_id] = tool_function
        logger.info(f"Registered tool function: {tool_id}")
    
    def get_tool_class(self, tool_id: str) -> Optional[Type]:
        """Get a tool class by ID."""
        return self.tool_classes.get(tool_id)
    
    def get_tool_function(self, tool_id: str) -> Optional[Callable]:
        """Get a tool function by ID."""
        return self.tool_functions.get(tool_id)
    
    async def create_tool(self, tool_id: str, **kwargs) -> Any:
        """Create a tool instance."""
        async with self._lock:
            if tool_id in self.tools:
                logger.warning(f"Tool {tool_id} already exists, returning existing instance")
                return self.tools[tool_id]
            
            tool_class = self.tool_classes.get(tool_id)
            if not tool_class:
                raise ValueError(f"Tool class not found for {tool_id}")
            
            try:
                # Create tool instance
                tool = tool_class(**kwargs)
                
                # Initialize tool if needed
                if hasattr(tool, 'initialize'):
                    await tool.initialize()
                
                # Store tool
                self.tools[tool_id] = tool
                self.tool_usage[tool_id] = {
                    "usage_count": 0,
                    "success_count": 0,
                    "error_count": 0,
                    "last_used": None,
                    "created_at": datetime.utcnow().isoformat()
                }
                
                logger.info(f"Created tool: {tool_id}")
                return tool
                
            except Exception as e:
                logger.error(f"Error creating tool {tool_id}: {e}")
                raise
    
    async def get_tool(self, tool_id: str) -> Optional[Any]:
        """Get a tool instance."""
        return self.tools.get(tool_id)
    
    async def get_tool_function(self, tool_id: str) -> Optional[Callable]:
        """Get a tool function."""
        return self.tool_functions.get(tool_id)
    
    async def execute_tool(self, tool_id: str, parameters: Dict[str, Any]) -> Any:
        """Execute a tool."""
        try:
            # Update usage statistics
            if tool_id in self.tool_usage:
                self.tool_usage[tool_id]["usage_count"] += 1
                self.tool_usage[tool_id]["last_used"] = datetime.utcnow().isoformat()
            
            # Try to get tool instance first
            tool = await self.get_tool(tool_id)
            if tool:
                if hasattr(tool, 'execute'):
                    result = await tool.execute(parameters)
                elif hasattr(tool, 'ainvoke'):
                    result = await tool.ainvoke(parameters)
                else:
                    raise ValueError(f"Tool {tool_id} does not have execute or ainvoke method")
            else:
                # Try to get tool function
                tool_function = await self.get_tool_function(tool_id)
                if tool_function:
                    if asyncio.iscoroutinefunction(tool_function):
                        result = await tool_function(**parameters)
                    else:
                        result = tool_function(**parameters)
                else:
                    raise ValueError(f"Tool {tool_id} not found")
            
            # Update success count
            if tool_id in self.tool_usage:
                self.tool_usage[tool_id]["success_count"] += 1
            
            return result
            
        except Exception as e:
            # Update error count
            if tool_id in self.tool_usage:
                self.tool_usage[tool_id]["error_count"] += 1
            
            logger.error(f"Error executing tool {tool_id}: {e}")
            raise
    
    async def get_all_tools(self) -> Dict[str, Any]:
        """Get all tool instances."""
        return self.tools.copy()
    
    async def get_all_tool_functions(self) -> Dict[str, Callable]:
        """Get all tool functions."""
        return self.tool_functions.copy()
    
    async def remove_tool(self, tool_id: str) -> bool:
        """Remove a tool instance."""
        async with self._lock:
            if tool_id in self.tools:
                # Cleanup tool if needed
                tool = self.tools[tool_id]
                if hasattr(tool, 'cleanup'):
                    try:
                        await tool.cleanup()
                    except Exception as e:
                        logger.error(f"Error cleaning up tool {tool_id}: {e}")
                
                del self.tools[tool_id]
                if tool_id in self.tool_usage:
                    del self.tool_usage[tool_id]
                
                logger.info(f"Removed tool: {tool_id}")
                return True
            return False
    
    async def get_tool_usage(self, tool_id: str) -> Optional[Dict[str, Any]]:
        """Get tool usage statistics."""
        return self.tool_usage.get(tool_id)
    
    async def get_all_tool_usage(self) -> Dict[str, Dict[str, Any]]:
        """Get usage statistics for all tools."""
        return self.tool_usage.copy()
    
    async def get_tool_health(self, tool_id: str) -> Dict[str, Any]:
        """Get tool health information."""
        if tool_id not in self.tool_usage:
            return {"status": "not_found", "healthy": False}
        
        usage = self.tool_usage[tool_id]
        error_rate = 0
        if usage["usage_count"] > 0:
            error_rate = usage["error_count"] / usage["usage_count"]
        
        return {
            "status": "active" if error_rate < 0.1 else "degraded",
            "healthy": error_rate < 0.1,
            "usage_count": usage["usage_count"],
            "success_count": usage["success_count"],
            "error_count": usage["error_count"],
            "error_rate": error_rate,
            "last_used": usage["last_used"],
            "created_at": usage["created_at"]
        }
    
    async def get_system_health(self) -> Dict[str, Any]:
        """Get overall system health."""
        total_tools = len(self.tools) + len(self.tool_functions)
        total_usage = sum(usage["usage_count"] for usage in self.tool_usage.values())
        total_errors = sum(usage["error_count"] for usage in self.tool_usage.values())
        
        overall_error_rate = 0
        if total_usage > 0:
            overall_error_rate = total_errors / total_usage
        
        return {
            "total_tools": total_tools,
            "total_usage": total_usage,
            "total_errors": total_errors,
            "overall_error_rate": overall_error_rate,
            "healthy": overall_error_rate < 0.1
        }
    
    async def get_tool_metrics(self) -> Dict[str, Any]:
        """Get tool metrics."""
        metrics = {
            "total_tools": len(self.tools) + len(self.tool_functions),
            "tool_instances": len(self.tools),
            "tool_functions": len(self.tool_functions),
            "usage_stats": {},
            "performance": {}
        }
        
        # Add usage statistics
        for tool_id, usage in self.tool_usage.items():
            metrics["usage_stats"][tool_id] = {
                "usage_count": usage["usage_count"],
                "success_count": usage["success_count"],
                "error_count": usage["error_count"],
                "error_rate": usage["error_count"] / max(usage["usage_count"], 1)
            }
        
        # Add performance metrics
        for tool_id, usage in self.tool_usage.items():
            if usage["usage_count"] > 0:
                metrics["performance"][tool_id] = {
                    "avg_success_rate": usage["success_count"] / usage["usage_count"],
                    "total_usage": usage["usage_count"],
                    "last_used": usage["last_used"]
                }
        
        return metrics
    
    async def cleanup_unused_tools(self, max_unused_hours: int = 24) -> int:
        """Clean up unused tools."""
        cutoff_time = datetime.utcnow().timestamp() - (max_unused_hours * 3600)
        cleaned_count = 0
        
        async with self._lock:
            tools_to_remove = []
            for tool_id, usage in self.tool_usage.items():
                if usage["last_used"]:
                    last_used = datetime.fromisoformat(usage["last_used"]).timestamp()
                    if last_used < cutoff_time and usage["usage_count"] == 0:
                        tools_to_remove.append(tool_id)
            
            for tool_id in tools_to_remove:
                await self.remove_tool(tool_id)
                cleaned_count += 1
        
        return cleaned_count
