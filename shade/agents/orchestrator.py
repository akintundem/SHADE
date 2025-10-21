"""Master orchestrator for multi-agent system."""

from typing import Dict, Any, List, Optional
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage
import asyncio
import logging
from .base_agent import BaseAgent
from .communication import MessageBus
from .event_agent import EventAgent
from .budget_agent import BudgetAgent
from .venue_agent import VenueAgent
from .vendor_agent import VendorAgent
from .risk_agent import RiskAgent
from .attendee_agent import AttendeeAgent
from .weather_agent import WeatherAgent
from .communication_agent import CommunicationAgent

logger = logging.getLogger(__name__)


class MasterOrchestrator:
    """Master orchestrator that coordinates all specialized agents."""
    
    def __init__(self):
        """Initialize the master orchestrator."""
        self.model = ChatOpenAI(model="gpt-4o", temperature=0.3)
        self.agents = {}
        self.conversation_histories = {}  # User-specific conversation histories
        self.global_context = {}
        self.message_bus = MessageBus()
        
        # Initialize all specialized agents
        self._initialize_agents()
        
        # Message bus will be started when needed
        
    def _initialize_agents(self):
        """Initialize all specialized agents."""
        self.agents = {
            "event": EventAgent(message_bus=self.message_bus),
            "budget": BudgetAgent(message_bus=self.message_bus),
            "venue": VenueAgent(message_bus=self.message_bus),
            "vendor": VendorAgent(message_bus=self.message_bus),
            "risk": RiskAgent(message_bus=self.message_bus),
            "attendee": AttendeeAgent(message_bus=self.message_bus),
            "weather": WeatherAgent(message_bus=self.message_bus),
            "communication": CommunicationAgent(message_bus=self.message_bus)
        }
        
        # Setup each agent
        for agent in self.agents.values():
            agent.setup_agent()
            
        logger.info(f"Initialized {len(self.agents)} specialized agents with communication")
    
    async def start_message_bus(self):
        """Start the message bus."""
        await self.message_bus.start()
    
    async def process_request(self, message: str, user_id: str, chat_id: str, event_id: Optional[str] = None) -> Dict[str, Any]:
        """Process a request using the multi-agent system."""
        try:
            # Get or create user-specific conversation history
            if user_id not in self.conversation_histories:
                self.conversation_histories[user_id] = []
            
            # Add user message to conversation history (keep only last 10 messages)
            self.conversation_histories[user_id].append({"role": "user", "content": message})
            if len(self.conversation_histories[user_id]) > 10:
                self.conversation_histories[user_id] = self.conversation_histories[user_id][-10:]
            
            # Clear and update global context (remove conversation history to avoid tool message issues)
            self.global_context = {
                "user_id": user_id,
                "chat_id": chat_id,
                "event_id": event_id,
                "message": message
                # Removed conversation_history to avoid tool message conflicts
            }
            
            # Determine which agents to involve
            involved_agents = await self._determine_agents(message)
            
            # Route to appropriate agents
            if len(involved_agents) == 1:
                # Single agent - direct routing
                agent_name = involved_agents[0]
                agent = self.agents[agent_name]
                result = await agent.process_message(message, self.global_context)
                
                # Add agent response to conversation history
                self.conversation_histories[user_id].append({"role": "assistant", "content": result["response"]})
                
                return {
                    "reply": result["response"],
                    "agent_used": agent_name,
                    "tool_used": result.get("tool_calls", []),
                    "data": result.get("context", {}),
                    "multi_agent": False
                }
            else:
                # Multi-agent coordination
                result = await self._coordinate_agents(message, involved_agents)
                
                # Add orchestrator response to conversation history
                self.conversation_histories[user_id].append({"role": "assistant", "content": result["reply"]})
                
                return result
                
        except Exception as e:
            logger.error(f"Error in master orchestrator: {e}")
            return {
                "reply": f"I encountered an error while processing your request: {str(e)}",
                "agent_used": "orchestrator",
                "tool_used": [],
                "data": {},
                "multi_agent": False,
                "error": str(e)
            }
    
    async def _determine_agents(self, message: str) -> List[str]:
        """Determine which agents should handle this message."""
        # Keywords for each agent
        agent_keywords = {
            "event": ["event", "planning", "timeline", "schedule", "coordinate", "organize"],
            "budget": ["budget", "cost", "price", "financial", "expense", "money"],
            "venue": ["venue", "location", "place", "space", "room", "hall"],
            "vendor": ["vendor", "caterer", "photographer", "decorator", "supplier", "service"],
            "risk": ["risk", "backup", "contingency", "safety", "emergency", "problem"],
            "attendee": ["attendee", "guest", "invite", "rsvp", "people", "list"],
            "weather": ["weather", "outdoor", "rain", "sunny", "forecast", "climate"],
            "communication": ["email", "invitation", "notification", "announce", "message"]
        }
        
        message_lower = message.lower()
        involved_agents = []
        
        for agent_name, keywords in agent_keywords.items():
            if any(keyword in message_lower for keyword in keywords):
                involved_agents.append(agent_name)
        
        # If no specific agents identified, use event agent as default
        if not involved_agents:
            involved_agents = ["event"]
        
        return involved_agents
    
    async def _coordinate_agents(self, message: str, agent_names: List[str]) -> Dict[str, Any]:
        """Coordinate multiple agents to handle a complex request."""
        try:
            # Get responses from all involved agents
            agent_responses = {}
            for agent_name in agent_names:
                agent = self.agents[agent_name]
                response = await agent.process_message(message, self.global_context)
                agent_responses[agent_name] = response
            
            # Use orchestrator to synthesize responses
            synthesis_prompt = f"""
            You're a helpful event planning assistant. Synthesize the team responses into ONE short, natural response.
            
            User said: {message}
            
            Team feedback:
            {self._format_agent_responses(agent_responses)}
            
            RULES:
            - MAX 1-2 sentences (under 25 words)
            - Natural, conversational tone
            - If multiple agents responded, combine their key points
            - Ask ONE follow-up question if needed
            - Use 1-2 emojis max
            
            Example: "Nov 24 looks great - 22°C and sunny! How many guests?"
            """
            
            synthesis_messages = [
                SystemMessage(content=synthesis_prompt),
                HumanMessage(content="Please synthesize the agent responses.")
            ]
            
            synthesis_response = await self.model.ainvoke(synthesis_messages)
            
            return {
                "reply": synthesis_response.content,
                "agent_used": f"Multi-agent: {', '.join(agent_names)}",
                "tool_used": [],
                "data": {
                    "involved_agents": agent_names,
                    "agent_responses": agent_responses
                },
                "multi_agent": True
            }
            
        except Exception as e:
            logger.error(f"Error coordinating agents: {e}")
            return {
                "reply": f"I encountered an error while coordinating multiple agents: {str(e)}",
                "agent_used": "orchestrator",
                "tool_used": [],
                "data": {},
                "multi_agent": True,
                "error": str(e)
            }
    
    def _format_agent_responses(self, agent_responses: Dict[str, Any]) -> str:
        """Format agent responses for synthesis."""
        formatted = []
        for agent_name, response in agent_responses.items():
            response_text = response.get('response', 'No response')
            tool_results = response.get('tool_results', [])
            
            # Add tool results to the response
            if tool_results:
                for tool_result in tool_results:
                    if 'result' in tool_result and isinstance(tool_result['result'], dict):
                        # Format tool results nicely
                        result_data = tool_result['result']
                        if 'forecast' in result_data:
                            # Weather data
                            forecast = result_data['forecast']
                            if 'current' in forecast:
                                current = forecast['current']
                                response_text += f" [WEATHER DATA: {current.get('condition', 'Unknown')}, {current.get('temperature', 'N/A')}°C]"
                        elif 'success' in result_data and result_data['success']:
                            response_text += f" [TOOL RESULT: {tool_result.get('tool_name', 'Unknown tool')} completed successfully]"
            
            formatted.append(f"{agent_name.upper()} Agent: {response_text}")
        return "\n".join(formatted)
    
    def get_agent_status(self) -> Dict[str, Any]:
        """Get status of all agents."""
        status = {}
        for name, agent in self.agents.items():
            status[name] = agent.get_agent_info()
        return status
    
    async def reset_conversation(self):
        """Reset conversation history."""
        self.conversation_history = []
        self.global_context = {}
        for agent in self.agents.values():
            agent.conversation_memory = []
            agent.agent_state = {}
    
    async def get_communication_stats(self) -> Dict[str, Any]:
        """Get communication statistics."""
        bus_stats = await self.message_bus.get_bus_stats()
        agent_stats = {}
        
        for name, agent in self.agents.items():
            if agent.communication_protocol:
                agent_stats[name] = agent.communication_protocol.get_protocol_stats()
        
        return {
            "message_bus": bus_stats,
            "agents": agent_stats
        }
