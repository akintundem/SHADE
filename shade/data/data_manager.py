"""Unified data manager for MongoDB, Redis, and Async Queue."""

import asyncio
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta

from .mongodb_service import MongoDBService
from .redis_cache import RedisCache
from .async_queue import AsyncQueue, TaskPriority

logger = logging.getLogger(__name__)


class DataManager:
    """Unified data manager for all data layer services."""
    
    def __init__(self):
        """Initialize data manager."""
        self.mongodb = MongoDBService()
        self.redis = RedisCache()
        self.queue = AsyncQueue(max_workers=5)
        self.initialized = False
        
    async def initialize(self):
        """Initialize all data services."""
        if self.initialized:
            return
        
        logger.info("Initializing data layer services...")
        
        # Initialize MongoDB
        await self.mongodb.connect()
        
        # Initialize Redis
        await self.redis.connect()
        
        # Initialize Async Queue
        await self.queue.start()
        
        # Register default task handlers
        await self._register_default_handlers()
        
        self.initialized = True
        logger.info("Data layer services initialized successfully")
    
    async def _register_default_handlers(self):
        """Register default task handlers."""
        # Email handler
        await self.queue.register_handler("send_email", self._handle_email_task)
        
        # Notification handler
        await self.queue.register_handler("send_notification", self._handle_notification_task)
        
        # Data processing handler
        await self.queue.register_handler("process_data", self._handle_data_processing_task)
        
        # Webhook handler
        await self.queue.register_handler("send_webhook", self._handle_webhook_task)
        
        # Cleanup handler
        await self.queue.register_handler("cleanup", self._handle_cleanup_task)
        
        logger.info("Default task handlers registered")
    
    # Task Handlers
    async def _handle_email_task(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Handle email sending task."""
        try:
            # Mock email sending - replace with actual email service
            to_email = payload.get("to_email")
            subject = payload.get("subject")
            body = payload.get("body")
            
            logger.info(f"Sending email to {to_email}: {subject}")
            
            # Simulate email sending
            await asyncio.sleep(0.1)
            
            return {
                "status": "sent",
                "to_email": to_email,
                "subject": subject,
                "sent_at": datetime.utcnow().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error sending email: {e}")
            raise
    
    async def _handle_notification_task(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Handle notification task."""
        try:
            user_id = payload.get("user_id")
            message = payload.get("message")
            notification_type = payload.get("notification_type")
            
            logger.info(f"Sending notification to user {user_id}: {message}")
            
            # Simulate notification sending
            await asyncio.sleep(0.1)
            
            return {
                "status": "sent",
                "user_id": user_id,
                "message": message,
                "notification_type": notification_type,
                "sent_at": datetime.utcnow().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error sending notification: {e}")
            raise
    
    async def _handle_data_processing_task(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Handle data processing task."""
        try:
            data = payload.get("data")
            processing_type = payload.get("processing_type")
            
            logger.info(f"Processing data: {processing_type}")
            
            # Simulate data processing
            await asyncio.sleep(0.5)
            
            return {
                "status": "processed",
                "processing_type": processing_type,
                "processed_at": datetime.utcnow().isoformat(),
                "result": {"processed_items": len(data) if isinstance(data, list) else 1}
            }
            
        except Exception as e:
            logger.error(f"Error processing data: {e}")
            raise
    
    async def _handle_webhook_task(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Handle webhook task."""
        try:
            url = payload.get("url")
            webhook_payload = payload.get("payload")
            
            logger.info(f"Sending webhook to {url}")
            
            # Simulate webhook sending
            await asyncio.sleep(0.2)
            
            return {
                "status": "sent",
                "url": url,
                "sent_at": datetime.utcnow().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error sending webhook: {e}")
            raise
    
    async def _handle_cleanup_task(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Handle cleanup task."""
        try:
            cleanup_type = payload.get("cleanup_type")
            parameters = payload.get("parameters", {})
            
            logger.info(f"Running cleanup: {cleanup_type}")
            
            cleaned_count = 0
            
            if cleanup_type == "conversations":
                cleaned_count = await self.mongodb.cleanup_old_data(days=parameters.get("days", 30))
            elif cleanup_type == "cache":
                cleaned_count = await self.redis.cleanup_expired()
            elif cleanup_type == "tasks":
                cleaned_count = await self.queue.cleanup_completed_tasks(
                    older_than_hours=parameters.get("hours", 24)
                )
            
            return {
                "status": "completed",
                "cleanup_type": cleanup_type,
                "cleaned_count": cleaned_count,
                "completed_at": datetime.utcnow().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error running cleanup: {e}")
            raise
    
    # High-level data operations
    async def save_conversation(self, user_id: str, chat_id: str, message: str, response: str, metadata: Dict[str, Any] = None) -> bool:
        """Save conversation with caching."""
        try:
            # Check if event loop is running
            try:
                loop = asyncio.get_running_loop()
                if loop.is_closed():
                    logger.warning("Event loop is closed, skipping conversation save")
                    return False
            except RuntimeError:
                logger.warning("No event loop running, skipping conversation save")
                return False
            
            # Save to MongoDB
            success = await self.mongodb.save_conversation(user_id, chat_id, message, response, metadata)
            
            if success:
                # Cache the conversation
                conversation_data = {
                    "user_id": user_id,
                    "chat_id": chat_id,
                    "message": message,
                    "response": response,
                    "metadata": metadata or {},
                    "timestamp": datetime.utcnow().isoformat()
                }
                await self.redis.cache_conversation(user_id, chat_id, conversation_data)
            
            return success
            
        except Exception as e:
            logger.error(f"Error saving conversation: {e}")
            return False
    
    async def get_conversation_history(self, user_id: str, chat_id: str, limit: int = 50) -> List[Dict[str, Any]]:
        """Get conversation history with caching."""
        try:
            # Try cache first
            cached_conversation = await self.redis.get_cached_conversation(user_id, chat_id)
            if cached_conversation:
                return [cached_conversation]
            
            # Get from MongoDB
            conversations = await self.mongodb.get_conversation_history(user_id, chat_id, limit)
            
            # Cache the result
            if conversations:
                await self.redis.cache_conversation(user_id, chat_id, conversations[0])
            
            return conversations
            
        except Exception as e:
            logger.error(f"Error getting conversation history: {e}")
            return []
    
    async def save_event(self, user_id: str, event_data: Dict[str, Any]) -> str:
        """Save event with caching."""
        try:
            # Save to MongoDB
            event_id = await self.mongodb.save_event(user_id, event_data)
            
            if event_id:
                # Cache the event
                await self.redis.set(f"event:{event_id}", event_data, ttl=3600)
            
            return event_id
            
        except Exception as e:
            logger.error(f"Error saving event: {e}")
            return ""
    
    async def get_event(self, event_id: str) -> Optional[Dict[str, Any]]:
        """Get event with caching."""
        try:
            # Try cache first
            cached_event = await self.redis.get(f"event:{event_id}")
            if cached_event:
                return cached_event
            
            # Get from MongoDB
            event = await self.mongodb.get_event(event_id)
            
            if event:
                # Cache the result
                await self.redis.set(f"event:{event_id}", event, ttl=3600)
            
            return event
            
        except Exception as e:
            logger.error(f"Error getting event: {e}")
            return None
    
    async def enqueue_background_task(self, task_name: str, payload: Dict[str, Any], priority: TaskPriority = TaskPriority.NORMAL) -> str:
        """Enqueue a background task."""
        try:
            return await self.queue.enqueue_task(task_name, payload, priority)
        except Exception as e:
            logger.error(f"Error enqueuing task: {e}")
            return ""
    
    async def get_system_stats(self) -> Dict[str, Any]:
        """Get comprehensive system statistics."""
        try:
            # Get MongoDB stats
            mongodb_stats = await self.mongodb.get_database_stats()
            
            # Get Redis stats
            redis_stats = await self.redis.get_cache_stats()
            
            # Get Queue stats
            queue_stats = await self.queue.get_queue_stats()
            
            return {
                "mongodb": mongodb_stats,
                "redis": redis_stats,
                "queue": queue_stats,
                "initialized": self.initialized,
                "timestamp": datetime.utcnow().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error getting system stats: {e}")
            return {"error": str(e)}
    
    async def cleanup_old_data(self, days: int = 30) -> Dict[str, int]:
        """Clean up old data across all services."""
        try:
            results = {}
            
            # Cleanup MongoDB
            mongodb_cleaned = await self.mongodb.cleanup_old_data(days)
            results["mongodb"] = mongodb_cleaned
            
            # Cleanup Redis
            redis_cleaned = await self.redis.cleanup_expired()
            results["redis"] = redis_cleaned
            
            # Cleanup Queue
            queue_cleaned = await self.queue.cleanup_completed_tasks(older_than_hours=days * 24)
            results["queue"] = queue_cleaned
            
            logger.info(f"Cleanup completed: {results}")
            return results
            
        except Exception as e:
            logger.error(f"Error cleaning up old data: {e}")
            return {"error": str(e)}
    
    async def health_check(self) -> Dict[str, Any]:
        """Perform health check on all services."""
        try:
            health_status = {
                "mongodb": {"status": "unknown", "connected": False},
                "redis": {"status": "unknown", "connected": False},
                "queue": {"status": "unknown", "running": False},
                "overall": "unknown"
            }
            
            # Check MongoDB
            try:
                mongodb_stats = await self.mongodb.get_database_stats()
                health_status["mongodb"] = {
                    "status": "healthy" if mongodb_stats.get("connected", False) else "unhealthy",
                    "connected": mongodb_stats.get("connected", False),
                    "stats": mongodb_stats
                }
            except Exception as e:
                health_status["mongodb"] = {"status": "error", "error": str(e)}
            
            # Check Redis
            try:
                redis_stats = await self.redis.get_cache_stats()
                health_status["redis"] = {
                    "status": "healthy" if redis_stats.get("connected", False) else "unhealthy",
                    "connected": redis_stats.get("connected", False),
                    "stats": redis_stats
                }
            except Exception as e:
                health_status["redis"] = {"status": "error", "error": str(e)}
            
            # Check Queue
            try:
                queue_stats = await self.queue.get_queue_stats()
                health_status["queue"] = {
                    "status": "healthy",
                    "running": True,
                    "stats": queue_stats
                }
            except Exception as e:
                health_status["queue"] = {"status": "error", "error": str(e)}
            
            # Determine overall status
            all_healthy = all(
                service["status"] in ["healthy", "unhealthy"] 
                for service in health_status.values() 
                if isinstance(service, dict) and "status" in service
            )
            
            health_status["overall"] = "healthy" if all_healthy else "unhealthy"
            
            return health_status
            
        except Exception as e:
            logger.error(f"Error performing health check: {e}")
            return {"error": str(e), "overall": "error"}
    
    async def shutdown(self):
        """Shutdown all data services."""
        try:
            logger.info("Shutting down data layer services...")
            
            # Stop queue
            await self.queue.stop()
            
            # Disconnect Redis
            await self.redis.disconnect()
            
            # Disconnect MongoDB
            await self.mongodb.disconnect()
            
            self.initialized = False
            logger.info("Data layer services shut down successfully")
            
        except Exception as e:
            logger.error(f"Error shutting down data services: {e}")
