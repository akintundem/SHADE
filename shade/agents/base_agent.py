"""Base agent class for all specialized agents."""

from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
from langchain_openai import ChatOpenAI
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolNode
from .communication import CommunicationProtocol, MessageBus, AgentMessage, MessageType, MessagePriority
import asyncio
import logging

logger = logging.getLogger(__name__)


class BaseAgent(ABC):
    """Base class for all specialized agents."""
    
    def __init__(self, agent_name: str, model_name: str = "gpt-4o", temperature: float = 0.7, message_bus: Optional[MessageBus] = None):
        """Initialize the base agent."""
        self.agent_name = agent_name
        self.model = ChatOpenAI(model=model_name, temperature=temperature)
        self.model_with_tools = None
        self.tools = []
        self.rag_system = None
        self.conversation_memory = []
        self.agent_state = {}
        self.message_bus = message_bus
        self.communication_protocol = None
        
    @abstractmethod
    def get_system_prompt(self) -> str:
        """Get the system prompt for this agent."""
        pass
    
    @abstractmethod
    def get_tools(self) -> List[Any]:
        """Get the tools available to this agent."""
        pass
    
    @abstractmethod
    def get_rag_system(self):
        """Get the RAG system for this agent."""
        pass
    
    def setup_agent(self):
        """Setup the agent with tools and RAG."""
        self.tools = self.get_tools()
        self.model_with_tools = self.model.bind_tools(self.tools)
        self.rag_system = self.get_rag_system()
        
        # Setup communication if message bus is available
        if self.message_bus:
            self.communication_protocol = CommunicationProtocol(self.agent_name, self.message_bus)
            # Communication protocol will be started when needed
        
    async def process_message(self, message: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """Process a message with this agent."""
        try:
            # Build messages starting with system prompt (with dynamic context)
            system_prompt = self.get_system_prompt(context) if hasattr(self, 'get_system_prompt') and callable(getattr(self, 'get_system_prompt')) else self.get_system_prompt()
            messages = [
                SystemMessage(content=system_prompt),
            ]
            
            # Add conversation history (keep only non-tool messages to avoid API errors)
            for msg in self.conversation_memory:
                # Only add HumanMessage and AIMessage, skip ToolMessage
                if msg.__class__.__name__ in ['HumanMessage', 'AIMessage']:
                    messages.append(msg)
            
            # Add current message
            messages.append(HumanMessage(content=message))
            
            # Get response from model with tools enabled
            response = await self.model_with_tools.ainvoke(messages)
            
            # Execute tool calls if any
            tool_results = []
            final_response = response.content if hasattr(response, 'content') else str(response)
            
            if hasattr(response, 'tool_calls') and response.tool_calls:
                print(f"🔧 {self.agent_name} executing {len(response.tool_calls)} tool calls")
                
                # Add the assistant response with tool calls to messages
                messages.append(response)
                
                # Execute tools and add tool messages
                from langchain_core.messages import ToolMessage
                for tool_call in response.tool_calls:
                    tool_result = await self.execute_tool(tool_call['name'], tool_call['args'])
                    tool_results.append(tool_result)
                    print(f"🔧 Tool {tool_call['name']} result: {tool_result}")
                    
                    # Add tool message with result
                    tool_message = ToolMessage(
                        content=str(tool_result.get('result', tool_result)),
                        tool_call_id=tool_call['id']
                    )
                    messages.append(tool_message)
                
                # Get final response from model with tool results
                final_response_obj = await self.model_with_tools.ainvoke(messages)
                final_response = final_response_obj.content if hasattr(final_response_obj, 'content') else str(final_response_obj)
            
            # Update conversation memory with only user message and final response (no tool messages)
            self.conversation_memory.append(HumanMessage(content=message))
            self.conversation_memory.append(AIMessage(content=final_response))
            
            # Keep only last 10 messages (5 exchanges)
            if len(self.conversation_memory) > 10:
                self.conversation_memory = self.conversation_memory[-10:]
            
            return {
                "agent_name": self.agent_name,
                "response": final_response,
                "tool_calls": getattr(response, 'tool_calls', []),
                "tool_results": tool_results,
                "context": context or {}
            }
            
        except Exception as e:
            logger.error(f"Error in {self.agent_name} agent: {e}")
            return {
                "agent_name": self.agent_name,
                "response": f"I encountered an error: {str(e)}",
                "tool_calls": [],
                "context": context or {},
                "error": str(e)
            }
    
    async def get_rag_context(self, message: str) -> str:
        """Get relevant context from RAG system."""
        if not self.rag_system:
            return ""
        
        try:
            # This would be implemented by each agent's RAG system
            return await self.rag_system.get_relevant_context(message)
        except Exception as e:
            logger.error(f"Error getting RAG context: {e}")
            return ""
    
    async def execute_tool(self, tool_name: str, tool_args: Dict[str, Any]) -> Dict[str, Any]:
        """Execute a tool for this agent."""
        try:
            # Find the tool
            tool = next((t for t in self.tools if t.name == tool_name), None)
            if not tool:
                return {"error": f"Tool {tool_name} not found"}
            
            # Execute the tool
            result = await tool.ainvoke(tool_args)
            return {"result": result, "tool_name": tool_name}
            
        except Exception as e:
            logger.error(f"Error executing tool {tool_name}: {e}")
            return {"error": str(e), "tool_name": tool_name}
    
    def _create_response_from_tool_result(self, tool_name: str, tool_result: Dict[str, Any]) -> str:
        """Create a response based on tool results when the model response is empty."""
        if tool_name == "get_weather_forecast" and "forecast" in tool_result:
            forecast = tool_result["forecast"]
            if "current" in forecast:
                current = forecast["current"]
                return f"Current weather: {current.get('condition', 'Unknown')}, {current.get('temperature', 'N/A')}°C"
            elif "daily" in forecast and forecast["daily"]:
                daily = forecast["daily"][0]
                return f"Weather forecast: {daily.get('condition', 'Unknown')}, High {daily.get('high', 'N/A')}°C, Low {daily.get('low', 'N/A')}°C"
        
        # Default response for other tools
        return f"Tool {tool_name} completed successfully."
    
    def get_agent_info(self) -> Dict[str, Any]:
        """Get information about this agent."""
        return {
            "name": self.agent_name,
            "tools": [tool.name for tool in self.tools],
            "has_rag": self.rag_system is not None,
            "conversation_length": len(self.conversation_memory),
            "has_communication": self.communication_protocol is not None
        }
    
    async def send_message_to_agent(self, recipient: str, message_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL) -> Optional[AgentMessage]:
        """Send a message to another agent."""
        if not self.communication_protocol:
            logger.warning(f"No communication protocol available for {self.agent_name}")
            return None
        
        return await self.communication_protocol.send_request(recipient, message_content, priority)
    
    async def broadcast_to_agents(self, message_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL):
        """Broadcast a message to all agents."""
        if not self.communication_protocol:
            logger.warning(f"No communication protocol available for {self.agent_name}")
            return
        
        await self.communication_protocol.broadcast_notification(message_content, priority)
    
    async def coordinate_with_agents(self, agents: List[str], coordination_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL) -> Dict[str, Any]:
        """Coordinate with multiple agents."""
        if not self.communication_protocol:
            logger.warning(f"No communication protocol available for {self.agent_name}")
            return {}
        
        return await self.communication_protocol.coordinate_with_agents(agents, coordination_content, priority)
    
    async def query_agent(self, recipient: str, query_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL) -> Optional[Dict[str, Any]]:
        """Query another agent for information."""
        if not self.communication_protocol:
            logger.warning(f"No communication protocol available for {self.agent_name}")
            return None
        
        return await self.communication_protocol.query_agent(recipient, query_content, priority)
    
    async def store_interaction(self, interaction_data: Dict[str, Any]) -> None:
        """Store interaction data for learning."""
        try:
            # This would be implemented by each agent's RAG system
            if self.rag_system and hasattr(self.rag_system, 'store_interaction_pattern'):
                await self.rag_system.store_interaction_pattern(
                    interaction_data.get("event_type", "UNKNOWN"),
                    interaction_data.get("planning_phase", "general"),
                    interaction_data.get("user_action", ""),
                    interaction_data.get("agent_response", ""),
                    interaction_data.get("success_metrics", {}),
                    interaction_data.get("context", {})
                )
        except Exception as e:
            logger.error(f"Error storing interaction: {e}")
    
    async def get_workflow_context(self) -> Dict[str, Any]:
        """Get workflow context for coordination."""
        return {
            "agent_name": self.agent_name,
            "conversation_length": len(self.conversation_memory),
            "has_rag": self.rag_system is not None,
            "tools_available": [tool.name for tool in self.tools]
        }
    
    async def validate_tool_result(self, tool_name: str, tool_result: Dict[str, Any]) -> Dict[str, Any]:
        """Validate tool result and provide feedback."""
        try:
            # Basic validation - can be overridden by specific agents
            if "error" in tool_result:
                return {
                    "is_valid": False,
                    "message": f"Tool {tool_name} failed: {tool_result['error']}",
                    "suggestion": "Please try again or provide different parameters"
                }
            
            return {
                "is_valid": True,
                "message": f"Tool {tool_name} executed successfully",
                "suggestion": None
            }
        except Exception as e:
            return {
                "is_valid": False,
                "message": f"Error validating tool result: {e}",
                "suggestion": "Please try again"
            }
    
    async def suggest_next_action(self, context: Dict[str, Any]) -> List[str]:
        """Suggest next actions based on context."""
        suggestions = []
        
        # Basic suggestions based on agent type
        if "event" in self.agent_name.lower():
            suggestions.extend([
                "Create or update event details",
                "Check event status",
                "Add venue requirements"
            ])
        elif "budget" in self.agent_name.lower():
            suggestions.extend([
                "Set budget constraints",
                "Track expenses",
                "Calculate costs"
            ])
        elif "venue" in self.agent_name.lower():
            suggestions.extend([
                "Search for venues",
                "Compare venue options",
                "Book venue"
            ])
        
        # Add context-specific suggestions
        if context.get("event_type"):
            event_type = context["event_type"]
            if event_type == "WEDDING":
                suggestions.append("Plan wedding timeline")
            elif event_type == "CONFERENCE":
                suggestions.append("Setup technical requirements")
            elif event_type == "BIRTHDAY":
                suggestions.append("Choose entertainment")
        
        return suggestions[:3]  # Return top 3 suggestions
