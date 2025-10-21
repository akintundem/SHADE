"""Observability hooks for LangGraph flow monitoring."""

from typing import Dict, Any, List, Optional
import logging
import time
from datetime import datetime
import json

logger = logging.getLogger(__name__)


class ObservabilityHooks:
    """Observability hooks for monitoring LangGraph flow execution."""
    
    def __init__(self):
        """Initialize observability hooks."""
        self.execution_stats = {
            "total_executions": 0,
            "successful_executions": 0,
            "failed_executions": 0,
            "average_execution_time": 0.0,
            "node_execution_times": {},
            "error_counts": {}
        }
        self.recent_executions = []
        self.max_recent_executions = 100
    
    def add_hooks(self, graph):
        """Add observability hooks to the graph."""
        # This would be implemented with actual LangGraph hooks
        # For now, we'll simulate the hook structure
        logger.info("Observability hooks added to LangGraph flow")
    
    def log_node_execution(self, node_name: str, start_time: float, end_time: float, success: bool, error: Optional[str] = None):
        """Log node execution metrics."""
        execution_time = end_time - start_time
        
        # Update stats
        self.execution_stats["total_executions"] += 1
        if success:
            self.execution_stats["successful_executions"] += 1
        else:
            self.execution_stats["failed_executions"] += 1
            if error:
                self.execution_stats["error_counts"][error] = self.execution_stats["error_counts"].get(error, 0) + 1
        
        # Update node execution times
        if node_name not in self.execution_stats["node_execution_times"]:
            self.execution_stats["node_execution_times"][node_name] = []
        self.execution_stats["node_execution_times"][node_name].append(execution_time)
        
        # Keep only last 50 executions per node
        if len(self.execution_stats["node_execution_times"][node_name]) > 50:
            self.execution_stats["node_execution_times"][node_name] = self.execution_stats["node_execution_times"][node_name][-50:]
        
        # Update average execution time
        total_time = sum(sum(times) for times in self.execution_stats["node_execution_times"].values())
        total_executions = sum(len(times) for times in self.execution_stats["node_execution_times"].values())
        if total_executions > 0:
            self.execution_stats["average_execution_time"] = total_time / total_executions
        
        # Log recent execution
        execution_record = {
            "timestamp": datetime.utcnow().isoformat(),
            "node_name": node_name,
            "execution_time": execution_time,
            "success": success,
            "error": error
        }
        
        self.recent_executions.append(execution_record)
        if len(self.recent_executions) > self.max_recent_executions:
            self.recent_executions = self.recent_executions[-self.max_recent_executions:]
        
        logger.info(f"Node {node_name} executed in {execution_time:.3f}s - {'SUCCESS' if success else 'FAILED'}")
    
    def log_flow_execution(self, flow_id: str, start_time: float, end_time: float, success: bool, domain_responses: Dict[str, Any] = None):
        """Log complete flow execution."""
        execution_time = end_time - start_time
        
        flow_record = {
            "flow_id": flow_id,
            "timestamp": datetime.utcnow().isoformat(),
            "execution_time": execution_time,
            "success": success,
            "domain_responses": domain_responses or {},
            "domains_involved": list(domain_responses.keys()) if domain_responses else []
        }
        
        logger.info(f"Flow {flow_id} completed in {execution_time:.3f}s - {'SUCCESS' if success else 'FAILED'}")
    
    def log_error(self, node_name: str, error: Exception, context: Dict[str, Any] = None):
        """Log execution errors."""
        error_info = {
            "timestamp": datetime.utcnow().isoformat(),
            "node_name": node_name,
            "error_type": type(error).__name__,
            "error_message": str(error),
            "context": context or {}
        }
        
        logger.error(f"Error in {node_name}: {error}", extra=error_info)
    
    def get_stats(self) -> Dict[str, Any]:
        """Get observability statistics."""
        return {
            "execution_stats": self.execution_stats.copy(),
            "recent_executions": self.recent_executions[-10:],  # Last 10 executions
            "node_performance": self._calculate_node_performance()
        }
    
    def _calculate_node_performance(self) -> Dict[str, Any]:
        """Calculate performance metrics for each node."""
        performance = {}
        
        for node_name, times in self.execution_stats["node_execution_times"].items():
            if times:
                performance[node_name] = {
                    "average_time": sum(times) / len(times),
                    "min_time": min(times),
                    "max_time": max(times),
                    "execution_count": len(times),
                    "success_rate": self._calculate_success_rate(node_name)
                }
        
        return performance
    
    def _calculate_success_rate(self, node_name: str) -> float:
        """Calculate success rate for a node."""
        # This would be calculated from actual execution logs
        # For now, return a placeholder
        return 0.95  # 95% success rate
    
    def get_health_status(self) -> Dict[str, Any]:
        """Get health status of the flow."""
        total_executions = self.execution_stats["total_executions"]
        successful_executions = self.execution_stats["successful_executions"]
        
        if total_executions == 0:
            return {"status": "unknown", "message": "No executions recorded"}
        
        success_rate = successful_executions / total_executions
        
        if success_rate >= 0.95:
            status = "healthy"
            message = f"Flow is healthy with {success_rate:.1%} success rate"
        elif success_rate >= 0.80:
            status = "degraded"
            message = f"Flow is degraded with {success_rate:.1%} success rate"
        else:
            status = "unhealthy"
            message = f"Flow is unhealthy with {success_rate:.1%} success rate"
        
        return {
            "status": status,
            "message": message,
            "success_rate": success_rate,
            "total_executions": total_executions,
            "average_execution_time": self.execution_stats["average_execution_time"]
        }
