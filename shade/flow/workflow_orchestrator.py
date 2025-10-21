"""Workflow orchestrator for automated agent coordination and tool chaining."""

from typing import Dict, Any, List, Optional, Callable
from dataclasses import dataclass
from enum import Enum
import asyncio
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class WorkflowState(Enum):
    """Workflow execution states."""
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    PAUSED = "paused"


class WorkflowStepType(Enum):
    """Types of workflow steps."""
    SEQUENTIAL = "sequential"
    PARALLEL = "parallel"
    CONDITIONAL = "conditional"
    USER_INPUT = "user_input"


@dataclass
class WorkflowStep:
    """Represents a step in a workflow."""
    step_id: str
    step_type: WorkflowStepType
    agent_name: str
    tool_name: str
    parameters: Dict[str, Any]
    conditions: Optional[Dict[str, Any]] = None
    dependencies: List[str] = None
    timeout: int = 300  # 5 minutes default


@dataclass
class WorkflowTemplate:
    """Template for a specific workflow type."""
    template_id: str
    name: str
    description: str
    event_type: str
    steps: List[WorkflowStep]
    estimated_duration: int  # minutes


@dataclass
class WorkflowExecution:
    """Represents an executing workflow."""
    execution_id: str
    template: WorkflowTemplate
    current_step: int
    state: WorkflowState
    results: Dict[str, Any]
    context: Dict[str, Any]
    started_at: datetime
    completed_at: Optional[datetime] = None


class WorkflowOrchestrator:
    """Orchestrates multi-agent workflows with automatic tool chaining."""
    
    def __init__(self):
        """Initialize the workflow orchestrator."""
        self.templates: Dict[str, WorkflowTemplate] = {}
        self.executions: Dict[str, WorkflowExecution] = {}
        self.agent_coordinator = None  # Will be set by dependency injection
        
        # Initialize workflow templates
        self._initialize_workflow_templates()
    
    def _initialize_workflow_templates(self):
        """Initialize common workflow templates."""
        # Wedding planning workflow
        wedding_template = WorkflowTemplate(
            template_id="wedding_planning",
            name="Wedding Planning Workflow",
            description="Complete wedding planning from start to finish",
            event_type="WEDDING",
            steps=[
                WorkflowStep(
                    step_id="create_event",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="event",
                    tool_name="start_event_creation",
                    parameters={}
                ),
                WorkflowStep(
                    step_id="set_budget",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="budget",
                    tool_name="create_budget",
                    parameters={},
                    dependencies=["create_event"]
                ),
                WorkflowStep(
                    step_id="find_venue",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="venue",
                    tool_name="search_venues",
                    parameters={},
                    dependencies=["set_budget"]
                ),
                WorkflowStep(
                    step_id="select_vendors",
                    step_type=WorkflowStepType.PARALLEL,
                    agent_name="vendor",
                    tool_name="search_vendors",
                    parameters={},
                    dependencies=["find_venue"]
                ),
                WorkflowStep(
                    step_id="check_weather",
                    step_type=WorkflowStepType.CONDITIONAL,
                    agent_name="weather",
                    tool_name="check_event_weather",
                    parameters={},
                    conditions={"venue_type": "outdoor"},
                    dependencies=["find_venue"]
                ),
                WorkflowStep(
                    step_id="create_timeline",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="event",
                    tool_name="create_timeline",
                    parameters={},
                    dependencies=["select_vendors"]
                )
            ],
            estimated_duration=120
        )
        
        # Corporate event workflow
        corporate_template = WorkflowTemplate(
            template_id="corporate_event",
            name="Corporate Event Workflow",
            description="Corporate event planning workflow",
            event_type="CONFERENCE",
            steps=[
                WorkflowStep(
                    step_id="create_event",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="event",
                    tool_name="start_event_creation",
                    parameters={}
                ),
                WorkflowStep(
                    step_id="find_venue",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="venue",
                    tool_name="search_venues",
                    parameters={},
                    dependencies=["create_event"]
                ),
                WorkflowStep(
                    step_id="setup_tech",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="event",
                    tool_name="enhance_event_details",
                    parameters={"technical_requirements": "AV, WiFi, Projector"},
                    dependencies=["find_venue"]
                ),
                WorkflowStep(
                    step_id="invite_attendees",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="attendee",
                    tool_name="add_attendee",
                    parameters={},
                    dependencies=["setup_tech"]
                ),
                WorkflowStep(
                    step_id="create_agenda",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="event",
                    tool_name="create_timeline",
                    parameters={},
                    dependencies=["invite_attendees"]
                )
            ],
            estimated_duration=90
        )
        
        # Birthday party workflow
        birthday_template = WorkflowTemplate(
            template_id="birthday_party",
            name="Birthday Party Workflow",
            description="Birthday party planning workflow",
            event_type="BIRTHDAY",
            steps=[
                WorkflowStep(
                    step_id="create_event",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="event",
                    tool_name="start_event_creation",
                    parameters={}
                ),
                WorkflowStep(
                    step_id="choose_theme",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="event",
                    tool_name="enhance_event_details",
                    parameters={},
                    dependencies=["create_event"]
                ),
                WorkflowStep(
                    step_id="find_venue",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="venue",
                    tool_name="search_venues",
                    parameters={},
                    dependencies=["choose_theme"]
                ),
                WorkflowStep(
                    step_id="book_entertainment",
                    step_type=WorkflowStepType.PARALLEL,
                    agent_name="vendor",
                    tool_name="search_vendors",
                    parameters={"category": "entertainment"},
                    dependencies=["find_venue"]
                ),
                WorkflowStep(
                    step_id="send_invitations",
                    step_type=WorkflowStepType.SEQUENTIAL,
                    agent_name="communication",
                    tool_name="send_notification",
                    parameters={},
                    dependencies=["book_entertainment"]
                )
            ],
            estimated_duration=60
        )
        
        self.templates = {
            "wedding_planning": wedding_template,
            "corporate_event": corporate_template,
            "birthday_party": birthday_template
        }
    
    async def start_workflow(self, template_id: str, context: Dict[str, Any], user_id: str, chat_id: str) -> str:
        """Start a new workflow execution."""
        if template_id not in self.templates:
            raise ValueError(f"Unknown workflow template: {template_id}")
        
        template = self.templates[template_id]
        execution_id = f"{template_id}_{user_id}_{datetime.utcnow().timestamp()}"
        
        execution = WorkflowExecution(
            execution_id=execution_id,
            template=template,
            current_step=0,
            state=WorkflowState.PENDING,
            results={},
            context=context,
            started_at=datetime.utcnow()
        )
        
        self.executions[execution_id] = execution
        
        # Start the workflow
        asyncio.create_task(self._execute_workflow(execution_id))
        
        logger.info(f"Started workflow {execution_id} for template {template_id}")
        return execution_id
    
    async def _execute_workflow(self, execution_id: str):
        """Execute a workflow."""
        execution = self.executions[execution_id]
        execution.state = WorkflowState.RUNNING
        
        try:
            while execution.current_step < len(execution.template.steps):
                step = execution.template.steps[execution.current_step]
                
                # Check dependencies
                if not await self._check_dependencies(execution, step):
                    logger.warning(f"Dependencies not met for step {step.step_id}")
                    break
                
                # Check conditions
                if not await self._check_conditions(execution, step):
                    logger.info(f"Conditions not met for step {step.step_id}, skipping")
                    execution.current_step += 1
                    continue
                
                # Execute step
                if step.step_type == WorkflowStepType.SEQUENTIAL:
                    await self._execute_sequential_step(execution, step)
                elif step.step_type == WorkflowStepType.PARALLEL:
                    await self._execute_parallel_step(execution, step)
                elif step.step_type == WorkflowStepType.CONDITIONAL:
                    await self._execute_conditional_step(execution, step)
                elif step.step_type == WorkflowStepType.USER_INPUT:
                    await self._pause_for_user_input(execution, step)
                    break
                
                execution.current_step += 1
            
            # Mark as completed if all steps done
            if execution.current_step >= len(execution.template.steps):
                execution.state = WorkflowState.COMPLETED
                execution.completed_at = datetime.utcnow()
                logger.info(f"Workflow {execution_id} completed successfully")
        
        except Exception as e:
            execution.state = WorkflowState.FAILED
            logger.error(f"Workflow {execution_id} failed: {e}")
    
    async def _check_dependencies(self, execution: WorkflowExecution, step: WorkflowStep) -> bool:
        """Check if step dependencies are met."""
        if not step.dependencies:
            return True
        
        for dep in step.dependencies:
            if dep not in execution.results:
                return False
        
        return True
    
    async def _check_conditions(self, execution: WorkflowExecution, step: WorkflowStep) -> bool:
        """Check if step conditions are met."""
        if not step.conditions:
            return True
        
        for condition_key, condition_value in step.conditions.items():
            context_value = execution.context.get(condition_key)
            if context_value != condition_value:
                return False
        
        return True
    
    async def _execute_sequential_step(self, execution: WorkflowExecution, step: WorkflowStep):
        """Execute a sequential step."""
        try:
            if self.agent_coordinator:
                result = await self.agent_coordinator.execute_agent_tool(
                    step.agent_name,
                    step.tool_name,
                    step.parameters,
                    execution.context
                )
                execution.results[step.step_id] = result
                logger.info(f"Executed sequential step {step.step_id}")
            else:
                logger.error("No agent coordinator available")
        except Exception as e:
            logger.error(f"Error executing sequential step {step.step_id}: {e}")
            execution.results[step.step_id] = {"error": str(e)}
    
    async def _execute_parallel_step(self, execution: WorkflowExecution, step: WorkflowStep):
        """Execute a parallel step."""
        try:
            if self.agent_coordinator:
                result = await self.agent_coordinator.execute_agent_tool(
                    step.agent_name,
                    step.tool_name,
                    step.parameters,
                    execution.context
                )
                execution.results[step.step_id] = result
                logger.info(f"Executed parallel step {step.step_id}")
            else:
                logger.error("No agent coordinator available")
        except Exception as e:
            logger.error(f"Error executing parallel step {step.step_id}: {e}")
            execution.results[step.step_id] = {"error": str(e)}
    
    async def _execute_conditional_step(self, execution: WorkflowExecution, step: WorkflowStep):
        """Execute a conditional step."""
        try:
            if self.agent_coordinator:
                result = await self.agent_coordinator.execute_agent_tool(
                    step.agent_name,
                    step.tool_name,
                    step.parameters,
                    execution.context
                )
                execution.results[step.step_id] = result
                logger.info(f"Executed conditional step {step.step_id}")
            else:
                logger.error("No agent coordinator available")
        except Exception as e:
            logger.error(f"Error executing conditional step {step.step_id}: {e}")
            execution.results[step.step_id] = {"error": str(e)}
    
    async def _pause_for_user_input(self, execution: WorkflowExecution, step: WorkflowStep):
        """Pause workflow for user input."""
        execution.state = WorkflowState.PAUSED
        logger.info(f"Workflow {execution.execution_id} paused for user input at step {step.step_id}")
    
    async def resume_workflow(self, execution_id: str, user_input: Dict[str, Any]):
        """Resume a paused workflow."""
        if execution_id not in self.executions:
            raise ValueError(f"Unknown workflow execution: {execution_id}")
        
        execution = self.executions[execution_id]
        if execution.state != WorkflowState.PAUSED:
            raise ValueError(f"Workflow {execution_id} is not paused")
        
        # Update context with user input
        execution.context.update(user_input)
        execution.state = WorkflowState.RUNNING
        
        # Continue execution
        asyncio.create_task(self._execute_workflow(execution_id))
        logger.info(f"Resumed workflow {execution_id}")
    
    async def get_workflow_status(self, execution_id: str) -> Dict[str, Any]:
        """Get workflow execution status."""
        if execution_id not in self.executions:
            return {"error": "Workflow not found"}
        
        execution = self.executions[execution_id]
        
        return {
            "execution_id": execution_id,
            "template_name": execution.template.name,
            "state": execution.state.value,
            "current_step": execution.current_step,
            "total_steps": len(execution.template.steps),
            "progress": execution.current_step / len(execution.template.steps) * 100,
            "started_at": execution.started_at.isoformat(),
            "completed_at": execution.completed_at.isoformat() if execution.completed_at else None,
            "results": execution.results
        }
    
    async def get_available_templates(self) -> List[Dict[str, Any]]:
        """Get available workflow templates."""
        return [
            {
                "template_id": template.template_id,
                "name": template.name,
                "description": template.description,
                "event_type": template.event_type,
                "estimated_duration": template.estimated_duration,
                "step_count": len(template.steps)
            }
            for template in self.templates.values()
        ]
    
    async def get_workflow_recommendations(self, event_type: str, context: Dict[str, Any]) -> List[str]:
        """Get workflow recommendations based on event type and context."""
        recommendations = []
        
        # Find matching templates
        matching_templates = [
            template for template in self.templates.values()
            if template.event_type == event_type
        ]
        
        if matching_templates:
            recommendations.append(f"Found {len(matching_templates)} workflow templates for {event_type}")
            
            for template in matching_templates:
                recommendations.append(f"- {template.name}: {template.description}")
        else:
            recommendations.append(f"No specific workflow templates for {event_type}")
            recommendations.append("Consider using the general event planning workflow")
        
        return recommendations
    
    def set_agent_coordinator(self, coordinator):
        """Set the agent coordinator for tool execution."""
        self.agent_coordinator = coordinator
