"""Agent message structure for inter-agent communication."""

from typing import Dict, Any, Optional, List
from datetime import datetime
from enum import Enum
import json
import uuid


class MessageType(Enum):
    """Types of messages between agents."""
    REQUEST = "request"
    RESPONSE = "response"
    NOTIFICATION = "notification"
    QUERY = "query"
    UPDATE = "update"
    ERROR = "error"


class MessagePriority(Enum):
    """Message priority levels."""
    LOW = 1
    NORMAL = 2
    HIGH = 3
    URGENT = 4


class AgentMessage:
    """Message structure for inter-agent communication."""
    
    def __init__(
        self,
        sender: str,
        recipient: str,
        message_type: MessageType,
        content: Dict[str, Any],
        priority: MessagePriority = MessagePriority.NORMAL,
        correlation_id: Optional[str] = None,
        reply_to: Optional[str] = None
    ):
        """Initialize an agent message."""
        self.id = str(uuid.uuid4())
        self.sender = sender
        self.recipient = recipient
        self.message_type = message_type
        self.content = content
        self.priority = priority
        self.correlation_id = correlation_id or str(uuid.uuid4())
        self.reply_to = reply_to
        self.timestamp = datetime.utcnow()
        self.status = "pending"
        self.retry_count = 0
        self.max_retries = 3
        
    def to_dict(self) -> Dict[str, Any]:
        """Convert message to dictionary."""
        return {
            "id": self.id,
            "sender": self.sender,
            "recipient": self.recipient,
            "message_type": self.message_type.value,
            "content": self.content,
            "priority": self.priority.value,
            "correlation_id": self.correlation_id,
            "reply_to": self.reply_to,
            "timestamp": self.timestamp.isoformat(),
            "status": self.status,
            "retry_count": self.retry_count,
            "max_retries": self.max_retries
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'AgentMessage':
        """Create message from dictionary."""
        message = cls(
            sender=data["sender"],
            recipient=data["recipient"],
            message_type=MessageType(data["message_type"]),
            content=data["content"],
            priority=MessagePriority(data["priority"]),
            correlation_id=data.get("correlation_id"),
            reply_to=data.get("reply_to")
        )
        message.id = data["id"]
        message.timestamp = datetime.fromisoformat(data["timestamp"])
        message.status = data["status"]
        message.retry_count = data["retry_count"]
        message.max_retries = data["max_retries"]
        return message
    
    def to_json(self) -> str:
        """Convert message to JSON string."""
        return json.dumps(self.to_dict(), default=str)
    
    @classmethod
    def from_json(cls, json_str: str) -> 'AgentMessage':
        """Create message from JSON string."""
        data = json.loads(json_str)
        return cls.from_dict(data)
    
    def mark_sent(self):
        """Mark message as sent."""
        self.status = "sent"
    
    def mark_delivered(self):
        """Mark message as delivered."""
        self.status = "delivered"
    
    def mark_failed(self):
        """Mark message as failed."""
        self.status = "failed"
    
    def increment_retry(self):
        """Increment retry count."""
        self.retry_count += 1
    
    def can_retry(self) -> bool:
        """Check if message can be retried."""
        return self.retry_count < self.max_retries
    
    def is_high_priority(self) -> bool:
        """Check if message is high priority."""
        return self.priority in [MessagePriority.HIGH, MessagePriority.URGENT]
    
    def __str__(self) -> str:
        """String representation of message."""
        return f"AgentMessage({self.sender} -> {self.recipient}, {self.message_type.value}, {self.priority.value})"
