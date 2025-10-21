"""Async queue service for background task processing."""

import asyncio
import logging
import json
from typing import Dict, Any, List, Optional, Callable, Union
from datetime import datetime, timedelta
from enum import Enum
import uuid
from dataclasses import dataclass, asdict
from concurrent.futures import ThreadPoolExecutor
import threading

logger = logging.getLogger(__name__)


class TaskPriority(Enum):
    """Task priority levels."""
    LOW = 1
    NORMAL = 2
    HIGH = 3
    URGENT = 4


class TaskStatus(Enum):
    """Task status levels."""
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


@dataclass
class Task:
    """Task data structure."""
    id: str
    name: str
    payload: Dict[str, Any]
    priority: TaskPriority
    status: TaskStatus
    created_at: datetime
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    retry_count: int = 0
    max_retries: int = 3
    error_message: Optional[str] = None
    result: Optional[Dict[str, Any]] = None
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        data = asdict(self)
        data["priority"] = self.priority.value
        data["status"] = self.status.value
        data["created_at"] = self.created_at.isoformat()
        if self.started_at:
            data["started_at"] = self.started_at.isoformat()
        if self.completed_at:
            data["completed_at"] = self.completed_at.isoformat()
        return data


class AsyncQueue:
    """Async queue service for background task processing."""
    
    def __init__(self, max_workers: int = 5):
        """Initialize async queue service."""
        self.max_workers = max_workers
        self.tasks: Dict[str, Task] = {}
        self.task_queue: asyncio.PriorityQueue = asyncio.PriorityQueue()
        self.workers: List[asyncio.Task] = []
        self.running = False
        self.task_handlers: Dict[str, Callable] = {}
        self.executor = ThreadPoolExecutor(max_workers=max_workers)
        self._lock = asyncio.Lock()
        
    async def start(self):
        """Start the async queue service."""
        if self.running:
            return
        
        self.running = True
        
        # Start worker tasks
        for i in range(self.max_workers):
            worker = asyncio.create_task(self._worker(f"worker-{i}"))
            self.workers.append(worker)
        
        logger.info(f"Async queue started with {self.max_workers} workers")
    
    async def stop(self):
        """Stop the async queue service."""
        if not self.running:
            return
        
        self.running = False
        
        # Cancel all workers
        for worker in self.workers:
            worker.cancel()
        
        # Wait for workers to finish
        await asyncio.gather(*self.workers, return_exceptions=True)
        self.workers.clear()
        
        # Shutdown executor
        self.executor.shutdown(wait=True)
        
        logger.info("Async queue stopped")
    
    async def _worker(self, worker_name: str):
        """Worker coroutine that processes tasks."""
        logger.info(f"Worker {worker_name} started")
        
        while self.running:
            try:
                # Get task from queue with timeout
                priority, task_id = await asyncio.wait_for(
                    self.task_queue.get(), timeout=1.0
                )
                
                task = self.tasks.get(task_id)
                if not task:
                    continue
                
                # Update task status
                async with self._lock:
                    task.status = TaskStatus.PROCESSING
                    task.started_at = datetime.utcnow()
                
                logger.info(f"Worker {worker_name} processing task {task_id}: {task.name}")
                
                # Process task
                try:
                    result = await self._process_task(task)
                    
                    # Update task status
                    async with self._lock:
                        task.status = TaskStatus.COMPLETED
                        task.completed_at = datetime.utcnow()
                        task.result = result
                    
                    logger.info(f"Task {task_id} completed successfully")
                    
                except Exception as e:
                    # Handle task failure
                    await self._handle_task_failure(task, str(e))
                    
            except asyncio.TimeoutError:
                # No tasks available, continue
                continue
            except Exception as e:
                logger.error(f"Worker {worker_name} error: {e}")
                await asyncio.sleep(1)
        
        logger.info(f"Worker {worker_name} stopped")
    
    async def _process_task(self, task: Task) -> Dict[str, Any]:
        """Process a single task."""
        handler = self.task_handlers.get(task.name)
        if not handler:
            raise ValueError(f"No handler registered for task: {task.name}")
        
        # Run handler in thread pool for CPU-intensive tasks
        if asyncio.iscoroutinefunction(handler):
            result = await handler(task.payload)
        else:
            result = await asyncio.get_event_loop().run_in_executor(
                self.executor, handler, task.payload
            )
        
        return result
    
    async def _handle_task_failure(self, task: Task, error_message: str):
        """Handle task failure with retry logic."""
        task.retry_count += 1
        task.error_message = error_message
        
        if task.retry_count < task.max_retries:
            # Retry task
            task.status = TaskStatus.PENDING
            task.started_at = None
            task.error_message = None
            
            # Add back to queue with higher priority
            await self.task_queue.put((task.priority.value + 1, task.id))
            
            logger.info(f"Task {task.id} retry {task.retry_count}/{task.max_retries}")
        else:
            # Mark as failed
            task.status = TaskStatus.FAILED
            task.completed_at = datetime.utcnow()
            
            logger.error(f"Task {task.id} failed after {task.max_retries} retries: {error_message}")
    
    async def enqueue_task(
        self,
        name: str,
        payload: Dict[str, Any],
        priority: TaskPriority = TaskPriority.NORMAL,
        max_retries: int = 3
    ) -> str:
        """Enqueue a new task."""
        task_id = str(uuid.uuid4())
        
        task = Task(
            id=task_id,
            name=name,
            payload=payload,
            priority=priority,
            status=TaskStatus.PENDING,
            created_at=datetime.utcnow(),
            max_retries=max_retries
        )
        
        async with self._lock:
            self.tasks[task_id] = task
        
        # Add to priority queue (lower number = higher priority)
        await self.task_queue.put((priority.value, task_id))
        
        logger.info(f"Enqueued task {task_id}: {name}")
        return task_id
    
    async def get_task(self, task_id: str) -> Optional[Task]:
        """Get task by ID."""
        return self.tasks.get(task_id)
    
    async def get_task_status(self, task_id: str) -> Optional[TaskStatus]:
        """Get task status."""
        task = self.tasks.get(task_id)
        return task.status if task else None
    
    async def cancel_task(self, task_id: str) -> bool:
        """Cancel a task."""
        task = self.tasks.get(task_id)
        if not task:
            return False
        
        if task.status in [TaskStatus.PENDING, TaskStatus.PROCESSING]:
            async with self._lock:
                task.status = TaskStatus.CANCELLED
                task.completed_at = datetime.utcnow()
            
            logger.info(f"Task {task_id} cancelled")
            return True
        
        return False
    
    async def get_tasks_by_status(self, status: TaskStatus) -> List[Task]:
        """Get tasks by status."""
        return [task for task in self.tasks.values() if task.status == status]
    
    async def get_tasks_by_priority(self, priority: TaskPriority) -> List[Task]:
        """Get tasks by priority."""
        return [task for task in self.tasks.values() if task.priority == priority]
    
    async def register_handler(self, task_name: str, handler: Callable):
        """Register a task handler."""
        self.task_handlers[task_name] = handler
        logger.info(f"Registered handler for task: {task_name}")
    
    async def get_queue_stats(self) -> Dict[str, Any]:
        """Get queue statistics."""
        total_tasks = len(self.tasks)
        pending_tasks = len([t for t in self.tasks.values() if t.status == TaskStatus.PENDING])
        processing_tasks = len([t for t in self.tasks.values() if t.status == TaskStatus.PROCESSING])
        completed_tasks = len([t for t in self.tasks.values() if t.status == TaskStatus.COMPLETED])
        failed_tasks = len([t for t in self.tasks.values() if t.status == TaskStatus.FAILED])
        
        return {
            "total_tasks": total_tasks,
            "pending_tasks": pending_tasks,
            "processing_tasks": processing_tasks,
            "completed_tasks": completed_tasks,
            "failed_tasks": failed_tasks,
            "queue_size": self.task_queue.qsize(),
            "active_workers": len([w for w in self.workers if not w.done()]),
            "registered_handlers": list(self.task_handlers.keys())
        }
    
    async def cleanup_completed_tasks(self, older_than_hours: int = 24) -> int:
        """Clean up completed tasks older than specified hours."""
        cutoff_time = datetime.utcnow() - timedelta(hours=older_than_hours)
        tasks_to_remove = []
        
        for task_id, task in self.tasks.items():
            if (task.status in [TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED] 
                and task.completed_at and task.completed_at < cutoff_time):
                tasks_to_remove.append(task_id)
        
        async with self._lock:
            for task_id in tasks_to_remove:
                del self.tasks[task_id]
        
        logger.info(f"Cleaned up {len(tasks_to_remove)} completed tasks")
        return len(tasks_to_remove)
    
    # Specialized task methods
    async def enqueue_email_task(self, to_email: str, subject: str, body: str, priority: TaskPriority = TaskPriority.NORMAL) -> str:
        """Enqueue email sending task."""
        return await self.enqueue_task(
            name="send_email",
            payload={
                "to_email": to_email,
                "subject": subject,
                "body": body
            },
            priority=priority
        )
    
    async def enqueue_notification_task(self, user_id: str, message: str, notification_type: str, priority: TaskPriority = TaskPriority.NORMAL) -> str:
        """Enqueue notification task."""
        return await self.enqueue_task(
            name="send_notification",
            payload={
                "user_id": user_id,
                "message": message,
                "notification_type": notification_type
            },
            priority=priority
        )
    
    async def enqueue_data_processing_task(self, data: Dict[str, Any], processing_type: str, priority: TaskPriority = TaskPriority.NORMAL) -> str:
        """Enqueue data processing task."""
        return await self.enqueue_task(
            name="process_data",
            payload={
                "data": data,
                "processing_type": processing_type
            },
            priority=priority
        )
    
    async def enqueue_webhook_task(self, url: str, payload: Dict[str, Any], priority: TaskPriority = TaskPriority.NORMAL) -> str:
        """Enqueue webhook task."""
        return await self.enqueue_task(
            name="send_webhook",
            payload={
                "url": url,
                "payload": payload
            },
            priority=priority
        )
    
    async def enqueue_cleanup_task(self, cleanup_type: str, parameters: Dict[str, Any], priority: TaskPriority = TaskPriority.LOW) -> str:
        """Enqueue cleanup task."""
        return await self.enqueue_task(
            name="cleanup",
            payload={
                "cleanup_type": cleanup_type,
                "parameters": parameters
            },
            priority=priority
        )
