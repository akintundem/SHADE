"""Communication protocol for inter-agent communication."""

from typing import Dict, Any, Optional, List, Callable
import asyncio
import logging
from datetime import datetime
from .agent_message import AgentMessage, MessageType, MessagePriority
from .message_bus import MessageBus

logger = logging.getLogger(__name__)


class CommunicationProtocol:
    """Communication protocol for agents."""
    
    def __init__(self, agent_name: str, message_bus: MessageBus):
        """Initialize communication protocol for an agent."""
        self.agent_name = agent_name
        self.message_bus = message_bus
        self.pending_requests: Dict[str, asyncio.Future] = {}
        self.message_handlers: Dict[MessageType, List[Callable]] = {}
        self.running = False
        
    async def start(self):
        """Start the communication protocol."""
        self.running = True
        await self.message_bus.subscribe(self.agent_name, self._handle_message)
        logger.info(f"Communication protocol started for {self.agent_name}")
    
    async def stop(self):
        """Stop the communication protocol."""
        self.running = False
        await self.message_bus.unsubscribe(self.agent_name, self._handle_message)
        logger.info(f"Communication protocol stopped for {self.agent_name}")
    
    async def _handle_message(self, message: AgentMessage):
        """Handle incoming messages."""
        try:
            if message.message_type in self.message_handlers:
                handlers = self.message_handlers[message.message_type]
                for handler in handlers:
                    await handler(message)
            
            # Handle request-response pattern
            if message.message_type == MessageType.REQUEST:
                await self._handle_request(message)
            elif message.message_type == MessageType.RESPONSE:
                await self._handle_response(message)
                
        except Exception as e:
            logger.error(f"Error handling message: {e}")
    
    async def _handle_request(self, message: AgentMessage):
        """Handle incoming requests."""
        # This would be implemented by each agent
        # to handle specific request types
        pass
    
    async def _handle_response(self, message: AgentMessage):
        """Handle incoming responses."""
        correlation_id = message.correlation_id
        if correlation_id in self.pending_requests:
            future = self.pending_requests[correlation_id]
            if not future.done():
                future.set_result(message)
            del self.pending_requests[correlation_id]
    
    def register_handler(self, message_type: MessageType, handler: Callable):
        """Register a message handler."""
        if message_type not in self.message_handlers:
            self.message_handlers[message_type] = []
        self.message_handlers[message_type].append(handler)
        logger.info(f"Handler registered for {message_type.value} in {self.agent_name}")
    
    async def send_request(self, recipient: str, request_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL, timeout: float = 30.0) -> Optional[AgentMessage]:
        """Send a request and wait for response."""
        try:
            correlation_id = await self.message_bus.send_request(
                self.agent_name,
                recipient,
                request_content,
                priority
            )
            
            # Create future for response
            future = asyncio.Future()
            self.pending_requests[correlation_id] = future
            
            # Wait for response with timeout
            response = await asyncio.wait_for(future, timeout=timeout)
            return response
            
        except asyncio.TimeoutError:
            logger.warning(f"Request timeout for {recipient}")
            if correlation_id in self.pending_requests:
                del self.pending_requests[correlation_id]
            return None
        except Exception as e:
            logger.error(f"Error sending request: {e}")
            return None
    
    async def send_response(self, recipient: str, response_content: Dict[str, Any], correlation_id: str, priority: MessagePriority = MessagePriority.NORMAL):
        """Send a response to a request."""
        await self.message_bus.send_response(
            self.agent_name,
            recipient,
            response_content,
            correlation_id,
            priority
        )
    
    async def send_notification(self, recipients: List[str], notification_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL):
        """Send a notification to multiple agents."""
        await self.message_bus.send_notification(
            self.agent_name,
            recipients,
            notification_content,
            priority
        )
    
    async def broadcast_notification(self, notification_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL):
        """Broadcast a notification to all agents."""
        await self.message_bus.broadcast_message(
            self.agent_name,
            MessageType.NOTIFICATION,
            notification_content,
            priority
        )
    
    async def query_agent(self, recipient: str, query_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL) -> Optional[Dict[str, Any]]:
        """Query another agent for information."""
        response = await self.send_request(recipient, query_content, priority)
        if response:
            return response.content
        return None
    
    async def coordinate_with_agents(self, agents: List[str], coordination_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL) -> Dict[str, Any]:
        """Coordinate with multiple agents."""
        results = {}
        
        # Send requests to all agents
        tasks = []
        for agent in agents:
            task = self.send_request(agent, coordination_content, priority)
            tasks.append((agent, task))
        
        # Wait for all responses
        for agent, task in tasks:
            try:
                response = await task
                if response:
                    results[agent] = response.content
                else:
                    results[agent] = {"error": "No response"}
            except Exception as e:
                results[agent] = {"error": str(e)}
        
        return results
    
    async def get_message_history(self, limit: int = 100) -> List[AgentMessage]:
        """Get message history for this agent."""
        return await self.message_bus.get_message_history(self.agent_name, limit)
    
    async def get_pending_messages(self) -> List[AgentMessage]:
        """Get pending messages for this agent."""
        return await self.message_bus.get_pending_messages(self.agent_name)
    
    def get_protocol_stats(self) -> Dict[str, Any]:
        """Get communication protocol statistics."""
        return {
            "agent_name": self.agent_name,
            "running": self.running,
            "pending_requests": len(self.pending_requests),
            "registered_handlers": sum(len(handlers) for handlers in self.message_handlers.values())
        }
