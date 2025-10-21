"""Message bus for inter-agent communication."""

from typing import Dict, Any, List, Optional, Callable
import asyncio
import logging
from datetime import datetime, timedelta
from .agent_message import AgentMessage, MessageType, MessagePriority

logger = logging.getLogger(__name__)


class MessageBus:
    """Message bus for inter-agent communication."""
    
    def __init__(self):
        """Initialize the message bus."""
        self.subscribers: Dict[str, List[Callable]] = {}
        self.message_queue: List[AgentMessage] = []
        self.message_history: List[AgentMessage] = []
        self.running = False
        self._lock = asyncio.Lock()
        
    async def start(self):
        """Start the message bus."""
        self.running = True
        asyncio.create_task(self._process_messages())
        logger.info("Message bus started")
    
    async def stop(self):
        """Stop the message bus."""
        self.running = False
        logger.info("Message bus stopped")
    
    async def subscribe(self, agent_name: str, handler: Callable):
        """Subscribe an agent to receive messages."""
        async with self._lock:
            if agent_name not in self.subscribers:
                self.subscribers[agent_name] = []
            self.subscribers[agent_name].append(handler)
            logger.info(f"Agent {agent_name} subscribed to message bus")
    
    async def unsubscribe(self, agent_name: str, handler: Callable):
        """Unsubscribe an agent from receiving messages."""
        async with self._lock:
            if agent_name in self.subscribers:
                if handler in self.subscribers[agent_name]:
                    self.subscribers[agent_name].remove(handler)
                    logger.info(f"Agent {agent_name} unsubscribed from message bus")
    
    async def send_message(self, message: AgentMessage) -> bool:
        """Send a message to an agent."""
        try:
            async with self._lock:
                # Add to queue
                self.message_queue.append(message)
                
                # Sort by priority
                self.message_queue.sort(key=lambda m: m.priority.value, reverse=True)
                
                logger.info(f"Message queued: {message}")
                return True
                
        except Exception as e:
            logger.error(f"Error sending message: {e}")
            return False
    
    async def broadcast_message(self, sender: str, message_type: MessageType, content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL) -> List[str]:
        """Broadcast a message to all agents."""
        sent_to = []
        
        for agent_name in self.subscribers.keys():
            message = AgentMessage(
                sender=sender,
                recipient=agent_name,
                message_type=message_type,
                content=content,
                priority=priority
            )
            
            if await self.send_message(message):
                sent_to.append(agent_name)
        
        return sent_to
    
    async def send_request(self, sender: str, recipient: str, request_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL) -> str:
        """Send a request message and return correlation ID."""
        message = AgentMessage(
            sender=sender,
            recipient=recipient,
            message_type=MessageType.REQUEST,
            content=request_content,
            priority=priority
        )
        
        await self.send_message(message)
        return message.correlation_id
    
    async def send_response(self, sender: str, recipient: str, response_content: Dict[str, Any], correlation_id: str, priority: MessagePriority = MessagePriority.NORMAL):
        """Send a response message."""
        message = AgentMessage(
            sender=sender,
            recipient=recipient,
            message_type=MessageType.RESPONSE,
            content=response_content,
            priority=priority,
            correlation_id=correlation_id
        )
        
        await self.send_message(message)
    
    async def send_notification(self, sender: str, recipients: List[str], notification_content: Dict[str, Any], priority: MessagePriority = MessagePriority.NORMAL):
        """Send a notification to multiple agents."""
        for recipient in recipients:
            message = AgentMessage(
                sender=sender,
                recipient=recipient,
                message_type=MessageType.NOTIFICATION,
                content=notification_content,
                priority=priority
            )
            await self.send_message(message)
    
    async def _process_messages(self):
        """Process messages in the queue."""
        while self.running:
            try:
                if self.message_queue:
                    async with self._lock:
                        message = self.message_queue.pop(0)
                    
                    await self._deliver_message(message)
                
                await asyncio.sleep(0.1)  # Small delay to prevent busy waiting
                
            except Exception as e:
                logger.error(f"Error processing messages: {e}")
                await asyncio.sleep(1)
    
    async def _deliver_message(self, message: AgentMessage):
        """Deliver a message to its recipient."""
        try:
            if message.recipient in self.subscribers:
                handlers = self.subscribers[message.recipient]
                
                for handler in handlers:
                    try:
                        await handler(message)
                        message.mark_delivered()
                        logger.info(f"Message delivered: {message}")
                        break
                    except Exception as e:
                        logger.error(f"Error in message handler: {e}")
                        message.increment_retry()
                        
                        if not message.can_retry():
                            message.mark_failed()
                            logger.error(f"Message failed after {message.max_retries} retries: {message}")
                        else:
                            # Re-queue for retry
                            await self.send_message(message)
            else:
                logger.warning(f"No subscribers for agent: {message.recipient}")
                message.mark_failed()
            
            # Add to history
            self.message_history.append(message)
            
            # Keep only last 1000 messages
            if len(self.message_history) > 1000:
                self.message_history = self.message_history[-1000:]
                
        except Exception as e:
            logger.error(f"Error delivering message: {e}")
            message.mark_failed()
    
    async def get_message_history(self, agent_name: Optional[str] = None, limit: int = 100) -> List[AgentMessage]:
        """Get message history for an agent or all agents."""
        if agent_name:
            return [msg for msg in self.message_history if msg.recipient == agent_name or msg.sender == agent_name][-limit:]
        else:
            return self.message_history[-limit:]
    
    async def get_pending_messages(self, agent_name: str) -> List[AgentMessage]:
        """Get pending messages for an agent."""
        return [msg for msg in self.message_queue if msg.recipient == agent_name]
    
    async def get_bus_stats(self) -> Dict[str, Any]:
        """Get message bus statistics."""
        return {
            "total_messages": len(self.message_history),
            "pending_messages": len(self.message_queue),
            "subscribers": len(self.subscribers),
            "running": self.running
        }
