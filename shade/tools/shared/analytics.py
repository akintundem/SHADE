"""Analytics for tracking interaction metrics and learning optimization."""

from typing import Dict, Any, List, Optional
from dataclasses import dataclass, field
from datetime import datetime, timedelta
import asyncio
import logging
import json

logger = logging.getLogger(__name__)


@dataclass
class InteractionMetrics:
    """Metrics for tracking agent interactions."""
    agent_id: str
    session_id: str
    user_id: str
    chat_id: str
    timestamp: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    request_count: int = 0
    response_time_ms: float = 0.0
    tool_success_rate: float = 0.0
    user_satisfaction_score: Optional[float] = None
    completion_rate: float = 0.0
    error_count: int = 0
    context_switches: int = 0
    workflow_steps_completed: int = 0


@dataclass
class LearningPattern:
    """Pattern extracted from successful interactions."""
    pattern_id: str
    event_type: str
    planning_phase: str
    user_actions: List[str]
    agent_responses: List[str]
    success_metrics: Dict[str, Any]
    frequency: int = 1
    last_used: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    effectiveness_score: float = 0.0


class InteractionAnalytics:
    """Analytics system for tracking and learning from interactions."""
    
    def __init__(self):
        """Initialize the analytics system."""
        self.metrics: List[InteractionMetrics] = []
        self.learning_patterns: List[LearningPattern] = []
        self._lock = asyncio.Lock()
    
    async def track_interaction(self, agent_id: str, session_id: str, user_id: str, 
                               chat_id: str, response_time_ms: float, 
                               tool_success: bool, user_satisfaction: Optional[float] = None) -> None:
        """Track an interaction."""
        async with self._lock:
            # Find existing metrics or create new
            existing_metrics = None
            for metrics in self.metrics:
                if (metrics.agent_id == agent_id and metrics.session_id == session_id and
                    metrics.user_id == user_id and metrics.chat_id == chat_id):
                    existing_metrics = metrics
                    break
            
            if existing_metrics:
                existing_metrics.request_count += 1
                existing_metrics.response_time_ms = response_time_ms
                if tool_success:
                    existing_metrics.tool_success_rate = (
                        (existing_metrics.tool_success_rate * (existing_metrics.request_count - 1) + 1.0) /
                        existing_metrics.request_count
                    )
                else:
                    existing_metrics.error_count += 1
                    existing_metrics.tool_success_rate = (
                        (existing_metrics.tool_success_rate * (existing_metrics.request_count - 1)) /
                        existing_metrics.request_count
                    )
                
                if user_satisfaction is not None:
                    existing_metrics.user_satisfaction_score = user_satisfaction
            else:
                # Create new metrics
                metrics = InteractionMetrics(
                    agent_id=agent_id,
                    session_id=session_id,
                    user_id=user_id,
                    chat_id=chat_id,
                    response_time_ms=response_time_ms,
                    tool_success_rate=1.0 if tool_success else 0.0,
                    user_satisfaction_score=user_satisfaction
                )
                metrics.request_count = 1
                if not tool_success:
                    metrics.error_count = 1
                    metrics.tool_success_rate = 0.0
                
                self.metrics.append(metrics)
    
    async def track_workflow_progress(self, agent_id: str, session_id: str, 
                                    user_id: str, chat_id: str, steps_completed: int) -> None:
        """Track workflow progress."""
        async with self._lock:
            for metrics in self.metrics:
                if (metrics.agent_id == agent_id and metrics.session_id == session_id and
                    metrics.user_id == user_id and metrics.chat_id == chat_id):
                    metrics.workflow_steps_completed = steps_completed
                    metrics.completion_rate = min(steps_completed / 10.0, 1.0)  # Assume 10 steps max
                    break
    
    async def track_context_switch(self, agent_id: str, session_id: str, 
                                  user_id: str, chat_id: str) -> None:
        """Track context switches."""
        async with self._lock:
            for metrics in self.metrics:
                if (metrics.agent_id == agent_id and metrics.session_id == session_id and
                    metrics.user_id == user_id and metrics.chat_id == chat_id):
                    metrics.context_switches += 1
                    break
    
    async def extract_learning_patterns(self, event_type: str, planning_phase: str,
                                     user_actions: List[str], agent_responses: List[str],
                                     success_metrics: Dict[str, Any]) -> str:
        """Extract learning patterns from successful interactions."""
        async with self._lock:
            # Check if similar pattern exists
            for pattern in self.learning_patterns:
                if (pattern.event_type == event_type and 
                    pattern.planning_phase == planning_phase and
                    pattern.user_actions == user_actions and
                    pattern.agent_responses == agent_responses):
                    # Update existing pattern
                    pattern.frequency += 1
                    pattern.last_used = datetime.utcnow().isoformat()
                    pattern.effectiveness_score = self._calculate_effectiveness_score(success_metrics)
                    return pattern.pattern_id
            
            # Create new pattern
            pattern_id = f"pattern_{len(self.learning_patterns) + 1}"
            pattern = LearningPattern(
                pattern_id=pattern_id,
                event_type=event_type,
                planning_phase=planning_phase,
                user_actions=user_actions,
                agent_responses=agent_responses,
                success_metrics=success_metrics,
                effectiveness_score=self._calculate_effectiveness_score(success_metrics)
            )
            
            self.learning_patterns.append(pattern)
            logger.info(f"Extracted learning pattern: {pattern_id}")
            return pattern_id
    
    def _calculate_effectiveness_score(self, success_metrics: Dict[str, Any]) -> float:
        """Calculate effectiveness score for a pattern."""
        score = 0.0
        
        # Event creation success
        if success_metrics.get("event_created", False):
            score += 0.3
        
        # User satisfaction
        satisfaction = success_metrics.get("user_satisfaction", 0.0)
        score += satisfaction * 0.3
        
        # Completion rate
        completion = success_metrics.get("completion_rate", 0.0)
        score += completion * 0.2
        
        # Response time (faster is better)
        response_time = success_metrics.get("response_time_ms", 0.0)
        if response_time > 0:
            score += max(0, 0.2 - (response_time / 10000.0))  # Normalize to 10 seconds
        
        return min(score, 1.0)
    
    async def get_agent_performance(self, agent_id: str, days: int = 7) -> Dict[str, Any]:
        """Get performance metrics for an agent."""
        cutoff_date = datetime.utcnow() - timedelta(days=days)
        
        agent_metrics = [
            m for m in self.metrics 
            if m.agent_id == agent_id and datetime.fromisoformat(m.timestamp) >= cutoff_date
        ]
        
        if not agent_metrics:
            return {"error": "No metrics found for agent"}
        
        total_requests = sum(m.request_count for m in agent_metrics)
        total_errors = sum(m.error_count for m in agent_metrics)
        avg_response_time = sum(m.response_time_ms for m in agent_metrics) / len(agent_metrics)
        avg_success_rate = sum(m.tool_success_rate for m in agent_metrics) / len(agent_metrics)
        avg_satisfaction = sum(m.user_satisfaction_score for m in agent_metrics 
                              if m.user_satisfaction_score is not None) / len(agent_metrics)
        
        return {
            "agent_id": agent_id,
            "period_days": days,
            "total_requests": total_requests,
            "total_errors": total_errors,
            "error_rate": total_errors / max(total_requests, 1),
            "avg_response_time_ms": avg_response_time,
            "avg_success_rate": avg_success_rate,
            "avg_satisfaction": avg_satisfaction,
            "total_sessions": len(agent_metrics)
        }
    
    async def get_learning_patterns(self, event_type: str, planning_phase: str) -> List[Dict[str, Any]]:
        """Get learning patterns for specific context."""
        patterns = [
            p for p in self.learning_patterns
            if p.event_type == event_type and p.planning_phase == planning_phase
        ]
        
        # Sort by effectiveness score
        patterns.sort(key=lambda x: x.effectiveness_score, reverse=True)
        
        return [
            {
                "pattern_id": p.pattern_id,
                "user_actions": p.user_actions,
                "agent_responses": p.agent_responses,
                "frequency": p.frequency,
                "effectiveness_score": p.effectiveness_score,
                "last_used": p.last_used
            }
            for p in patterns
        ]
    
    async def get_system_analytics(self, days: int = 7) -> Dict[str, Any]:
        """Get overall system analytics."""
        cutoff_date = datetime.utcnow() - timedelta(days=days)
        
        recent_metrics = [
            m for m in self.metrics 
            if datetime.fromisoformat(m.timestamp) >= cutoff_date
        ]
        
        if not recent_metrics:
            return {"error": "No metrics found for period"}
        
        # Overall metrics
        total_requests = sum(m.request_count for m in recent_metrics)
        total_errors = sum(m.error_count for m in recent_metrics)
        avg_response_time = sum(m.response_time_ms for m in recent_metrics) / len(recent_metrics)
        
        # Agent performance
        agent_performance = {}
        for metrics in recent_metrics:
            if metrics.agent_id not in agent_performance:
                agent_performance[metrics.agent_id] = {
                    "requests": 0,
                    "errors": 0,
                    "response_times": []
                }
            
            agent_performance[metrics.agent_id]["requests"] += metrics.request_count
            agent_performance[metrics.agent_id]["errors"] += metrics.error_count
            agent_performance[metrics.agent_id]["response_times"].append(metrics.response_time_ms)
        
        # Calculate agent stats
        for agent_id, stats in agent_performance.items():
            stats["error_rate"] = stats["errors"] / max(stats["requests"], 1)
            stats["avg_response_time"] = sum(stats["response_times"]) / len(stats["response_times"])
        
        # Learning patterns
        pattern_count = len(self.learning_patterns)
        effective_patterns = len([p for p in self.learning_patterns if p.effectiveness_score > 0.7])
        
        return {
            "period_days": days,
            "total_requests": total_requests,
            "total_errors": total_errors,
            "overall_error_rate": total_errors / max(total_requests, 1),
            "avg_response_time_ms": avg_response_time,
            "agent_performance": agent_performance,
            "learning_patterns": {
                "total_patterns": pattern_count,
                "effective_patterns": effective_patterns,
                "effectiveness_rate": effective_patterns / max(pattern_count, 1)
            }
        }
    
    async def cleanup_old_metrics(self, days: int = 30) -> int:
        """Clean up old metrics."""
        cutoff_date = datetime.utcnow() - timedelta(days=days)
        
        async with self._lock:
            old_metrics = [
                m for m in self.metrics 
                if datetime.fromisoformat(m.timestamp) < cutoff_date
            ]
            
            for metric in old_metrics:
                self.metrics.remove(metric)
            
            logger.info(f"Cleaned up {len(old_metrics)} old metrics")
            return len(old_metrics)
    
    async def export_analytics(self, file_path: str) -> bool:
        """Export analytics to file."""
        try:
            data = {
                "metrics": [
                    {
                        "agent_id": m.agent_id,
                        "session_id": m.session_id,
                        "user_id": m.user_id,
                        "chat_id": m.chat_id,
                        "timestamp": m.timestamp,
                        "request_count": m.request_count,
                        "response_time_ms": m.response_time_ms,
                        "tool_success_rate": m.tool_success_rate,
                        "user_satisfaction_score": m.user_satisfaction_score,
                        "completion_rate": m.completion_rate,
                        "error_count": m.error_count,
                        "context_switches": m.context_switches,
                        "workflow_steps_completed": m.workflow_steps_completed
                    }
                    for m in self.metrics
                ],
                "learning_patterns": [
                    {
                        "pattern_id": p.pattern_id,
                        "event_type": p.event_type,
                        "planning_phase": p.planning_phase,
                        "user_actions": p.user_actions,
                        "agent_responses": p.agent_responses,
                        "success_metrics": p.success_metrics,
                        "frequency": p.frequency,
                        "last_used": p.last_used,
                        "effectiveness_score": p.effectiveness_score
                    }
                    for p in self.learning_patterns
                ]
            }
            
            with open(file_path, 'w') as f:
                json.dump(data, f, indent=2)
            
            logger.info(f"Exported analytics to {file_path}")
            return True
            
        except Exception as e:
            logger.error(f"Error exporting analytics: {e}")
            return False
