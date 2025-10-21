"""Agent coordinator for cross-agent collaboration and tool execution."""

from typing import Dict, Any, List, Optional, Callable
from dataclasses import dataclass
from enum import Enum
import asyncio
import logging
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)


class CoordinationPattern(Enum):
    """Patterns for agent coordination."""
    SEQUENTIAL = "sequential"
    PARALLEL = "parallel"
    CONDITIONAL = "conditional"
    REQUEST_RESPONSE = "request_response"
    BROADCAST = "broadcast"


@dataclass
class AgentRequest:
    """Request from one agent to another."""
    request_id: str
    from_agent: str
    to_agent: str
    request_type: str
    parameters: Dict[str, Any]
    context: Dict[str, Any]
    timestamp: datetime
    timeout: int = 300  # 5 minutes


@dataclass
class AgentResponse:
    """Response from an agent to a request."""
    request_id: str
    from_agent: str
    to_agent: str
    response_data: Dict[str, Any]
    success: bool
    timestamp: datetime
    error_message: Optional[str] = None


class AgentCoordinator:
    """Coordinates cross-agent collaboration and tool execution."""
    
    def __init__(self, agents: Dict[str, Any]):
        """Initialize the agent coordinator."""
        self.agents = agents
        self.pending_requests: Dict[str, AgentRequest] = {}
        self.request_responses: Dict[str, AgentResponse] = {}
        self.coordination_context: Dict[str, Any] = {}
        self._lock = asyncio.Lock()
    
    async def execute_agent_tool(self, agent_name: str, tool_name: str, parameters: Dict[str, Any], context: Dict[str, Any]) -> Dict[str, Any]:
        """Execute a tool on a specific agent."""
        try:
            if agent_name not in self.agents:
                return {"error": f"Agent {agent_name} not found"}
            
            agent = self.agents[agent_name]
            
            # Update agent context
            if hasattr(agent, 'conversation_memory'):
                # Add context to agent's memory
                agent.conversation_memory.append({
                    "role": "system",
                    "content": f"Workflow context: {context}"
                })
            
            # Execute the tool
            result = await agent.execute_tool(tool_name, parameters)
            
            logger.info(f"Executed {tool_name} on {agent_name}")
            return result
            
        except Exception as e:
            logger.error(f"Error executing {tool_name} on {agent_name}: {e}")
            return {"error": str(e)}
    
    async def coordinate_agents(self, coordination_pattern: CoordinationPattern, agents: List[str], coordination_data: Dict[str, Any]) -> Dict[str, Any]:
        """Coordinate multiple agents based on pattern."""
        try:
            if coordination_pattern == CoordinationPattern.SEQUENTIAL:
                return await self._coordinate_sequential(agents, coordination_data)
            elif coordination_pattern == CoordinationPattern.PARALLEL:
                return await self._coordinate_parallel(agents, coordination_data)
            elif coordination_pattern == CoordinationPattern.CONDITIONAL:
                return await self._coordinate_conditional(agents, coordination_data)
            elif coordination_pattern == CoordinationPattern.REQUEST_RESPONSE:
                return await self._coordinate_request_response(agents, coordination_data)
            elif coordination_pattern == CoordinationPattern.BROADCAST:
                return await self._coordinate_broadcast(agents, coordination_data)
            else:
                return {"error": f"Unknown coordination pattern: {coordination_pattern}"}
                
        except Exception as e:
            logger.error(f"Error in agent coordination: {e}")
            return {"error": str(e)}
    
    async def _coordinate_sequential(self, agents: List[str], coordination_data: Dict[str, Any]) -> Dict[str, Any]:
        """Coordinate agents sequentially."""
        results = {}
        context = coordination_data.get("context", {})
        
        for agent_name in agents:
            if agent_name not in self.agents:
                results[agent_name] = {"error": f"Agent {agent_name} not found"}
                continue
            
            agent = self.agents[agent_name]
            tool_name = coordination_data.get("tool_name", "process_message")
            parameters = coordination_data.get("parameters", {})
            
            try:
                result = await agent.execute_tool(tool_name, parameters)
                results[agent_name] = result
                
                # Update context with result for next agent
                if "result" in result:
                    context.update(result["result"])
                
            except Exception as e:
                results[agent_name] = {"error": str(e)}
        
        return {
            "pattern": "sequential",
            "results": results,
            "context": context
        }
    
    async def _coordinate_parallel(self, agents: List[str], coordination_data: Dict[str, Any]) -> Dict[str, Any]:
        """Coordinate agents in parallel."""
        context = coordination_data.get("context", {})
        
        # Create tasks for parallel execution
        tasks = []
        for agent_name in agents:
            if agent_name in self.agents:
                agent = self.agents[agent_name]
                tool_name = coordination_data.get("tool_name", "process_message")
                parameters = coordination_data.get("parameters", {})
                
                task = self._execute_agent_task(agent_name, agent, tool_name, parameters)
                tasks.append((agent_name, task))
        
        # Execute all tasks in parallel
        results = {}
        if tasks:
            task_results = await asyncio.gather(*[task for _, task in tasks], return_exceptions=True)
            
            for i, (agent_name, _) in enumerate(tasks):
                if isinstance(task_results[i], Exception):
                    results[agent_name] = {"error": str(task_results[i])}
                else:
                    results[agent_name] = task_results[i]
        
        return {
            "pattern": "parallel",
            "results": results,
            "context": context
        }
    
    async def _coordinate_conditional(self, agents: List[str], coordination_data: Dict[str, Any]) -> Dict[str, Any]:
        """Coordinate agents conditionally based on context."""
        context = coordination_data.get("context", {})
        conditions = coordination_data.get("conditions", {})
        results = {}
        
        for agent_name in agents:
            if agent_name not in self.agents:
                results[agent_name] = {"error": f"Agent {agent_name} not found"}
                continue
            
            # Check conditions for this agent
            should_execute = True
            agent_conditions = conditions.get(agent_name, {})
            
            for condition_key, condition_value in agent_conditions.items():
                if context.get(condition_key) != condition_value:
                    should_execute = False
                    break
            
            if should_execute:
                agent = self.agents[agent_name]
                tool_name = coordination_data.get("tool_name", "process_message")
                parameters = coordination_data.get("parameters", {})
                
                try:
                    result = await agent.execute_tool(tool_name, parameters)
                    results[agent_name] = result
                except Exception as e:
                    results[agent_name] = {"error": str(e)}
            else:
                results[agent_name] = {"skipped": "Conditions not met"}
        
        return {
            "pattern": "conditional",
            "results": results,
            "context": context
        }
    
    async def _coordinate_request_response(self, agents: List[str], coordination_data: Dict[str, Any]) -> Dict[str, Any]:
        """Coordinate agents with request-response pattern."""
        if len(agents) < 2:
            return {"error": "Request-response requires at least 2 agents"}
        
        requester = agents[0]
        responder = agents[1]
        
        if requester not in self.agents or responder not in self.agents:
            return {"error": "Required agents not found"}
        
        # Create request
        request_id = f"req_{datetime.utcnow().timestamp()}"
        request = AgentRequest(
            request_id=request_id,
            from_agent=requester,
            to_agent=responder,
            request_type=coordination_data.get("request_type", "general"),
            parameters=coordination_data.get("parameters", {}),
            context=coordination_data.get("context", {}),
            timestamp=datetime.utcnow()
        )
        
        self.pending_requests[request_id] = request
        
        # Execute request
        try:
            requester_agent = self.agents[requester]
            responder_agent = self.agents[responder]
            
            # Send request (simplified - in reality this would use message bus)
            request_result = await requester_agent.execute_tool("send_request", {
                "request_id": request_id,
                "to_agent": responder,
                "request_data": request.parameters
            })
            
            # Process response
            response_result = await responder_agent.execute_tool("process_request", {
                "request_id": request_id,
                "request_data": request.parameters
            })
            
            # Create response
            response = AgentResponse(
                request_id=request_id,
                from_agent=responder,
                to_agent=requester,
                response_data=response_result,
                success=True,
                timestamp=datetime.utcnow()
            )
            
            self.request_responses[request_id] = response
            
            return {
                "pattern": "request_response",
                "request_id": request_id,
                "requester": requester,
                "responder": responder,
                "request_result": request_result,
                "response_result": response_result
            }
            
        except Exception as e:
            logger.error(f"Error in request-response coordination: {e}")
            return {"error": str(e)}
    
    async def _coordinate_broadcast(self, agents: List[str], coordination_data: Dict[str, Any]) -> Dict[str, Any]:
        """Coordinate agents with broadcast pattern."""
        context = coordination_data.get("context", {})
        broadcast_data = coordination_data.get("broadcast_data", {})
        results = {}
        
        # Create broadcast tasks
        tasks = []
        for agent_name in agents:
            if agent_name in self.agents:
                agent = self.agents[agent_name]
                task = self._broadcast_to_agent(agent_name, agent, broadcast_data, context)
                tasks.append((agent_name, task))
        
        # Execute broadcast in parallel
        if tasks:
            task_results = await asyncio.gather(*[task for _, task in tasks], return_exceptions=True)
            
            for i, (agent_name, _) in enumerate(tasks):
                if isinstance(task_results[i], Exception):
                    results[agent_name] = {"error": str(task_results[i])}
                else:
                    results[agent_name] = task_results[i]
        
        return {
            "pattern": "broadcast",
            "results": results,
            "context": context
        }
    
    async def _execute_agent_task(self, agent_name: str, agent: Any, tool_name: str, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """Execute a task on an agent."""
        try:
            result = await agent.execute_tool(tool_name, parameters)
            return result
        except Exception as e:
            return {"error": str(e)}
    
    async def _broadcast_to_agent(self, agent_name: str, agent: Any, broadcast_data: Dict[str, Any], context: Dict[str, Any]) -> Dict[str, Any]:
        """Broadcast data to an agent."""
        try:
            # Add broadcast data to context
            updated_context = {**context, **broadcast_data}
            
            # Execute broadcast on agent
            result = await agent.execute_tool("process_broadcast", {
                "broadcast_data": broadcast_data,
                "context": updated_context
            })
            
            return result
        except Exception as e:
            return {"error": str(e)}
    
    async def get_coordination_context(self) -> Dict[str, Any]:
        """Get current coordination context."""
        async with self._lock:
            return self.coordination_context.copy()
    
    async def update_coordination_context(self, updates: Dict[str, Any]) -> None:
        """Update coordination context."""
        async with self._lock:
            self.coordination_context.update(updates)
    
    async def get_pending_requests(self) -> List[Dict[str, Any]]:
        """Get pending requests."""
        async with self._lock:
            return [
                {
                    "request_id": req.request_id,
                    "from_agent": req.from_agent,
                    "to_agent": req.to_agent,
                    "request_type": req.request_type,
                    "timestamp": req.timestamp.isoformat()
                }
                for req in self.pending_requests.values()
            ]
    
    async def get_request_response(self, request_id: str) -> Optional[Dict[str, Any]]:
        """Get response for a request."""
        if request_id in self.request_responses:
            response = self.request_responses[request_id]
            return {
                "request_id": response.request_id,
                "from_agent": response.from_agent,
                "to_agent": response.to_agent,
                "response_data": response.response_data,
                "success": response.success,
                "timestamp": response.timestamp.isoformat(),
                "error_message": response.error_message
            }
        return None
    
    async def cleanup_old_requests(self, max_age_hours: int = 24) -> int:
        """Clean up old requests and responses."""
        cutoff_time = datetime.utcnow() - timedelta(hours=max_age_hours)
        cleaned_count = 0
        
        async with self._lock:
            # Clean up old requests
            old_requests = [
                req_id for req_id, req in self.pending_requests.items()
                if req.timestamp < cutoff_time
            ]
            for req_id in old_requests:
                del self.pending_requests[req_id]
                cleaned_count += 1
            
            # Clean up old responses
            old_responses = [
                req_id for req_id, resp in self.request_responses.items()
                if resp.timestamp < cutoff_time
            ]
            for req_id in old_responses:
                del self.request_responses[req_id]
                cleaned_count += 1
        
        return cleaned_count
