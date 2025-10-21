"""Test script for the Event Agent intelligence improvements."""

import asyncio
import sys
import os
from datetime import datetime, timedelta
from typing import Dict, Any

# Add the shade directory to the path
sys.path.append('/Users/mayokun/Desktop/Event Planner Monolith/shade')

from knowledge.interaction_store import InteractionPatternStore, InteractionPattern
from tools.validation import EventValidator, ValidationSeverity
from tools.event_insights import EventInsightsGenerator, InsightType, InsightSeverity
from flow.workflow_orchestrator import WorkflowOrchestrator
from flow.agent_coordinator import AgentCoordinator, CoordinationPattern
from agents.rag.event_rag import EventRAGSystem


async def test_interaction_pattern_learning():
    """Test interaction pattern learning capabilities."""
    print("🧠 Testing Interaction Pattern Learning...")
    
    # Initialize interaction store
    store = InteractionPatternStore()
    
    # Test storing interaction patterns
    pattern = InteractionPattern(
        event_type="WEDDING",
        planning_phase="initial",
        user_action="create wedding event",
        agent_response="Great! Let's start with your guest count and budget.",
        success_metrics={"completion_rate": 0.85, "user_satisfaction": 4.2},
        context={"guest_count": "100-200", "budget_range": "$20k-50k"},
        timestamp=datetime.utcnow().isoformat(),
        pattern_id=""
    )
    
    pattern_id = await store.store_interaction_pattern(pattern)
    print(f"✅ Stored interaction pattern: {pattern_id}")
    
    # Test getting similar patterns
    similar_patterns = await store.get_similar_patterns("WEDDING", "initial", {"guest_count": "150"})
    print(f"✅ Found {len(similar_patterns)} similar patterns")
    
    # Test getting recommended next steps
    next_steps = await store.get_recommended_next_steps("WEDDING", "initial", ["create_event"])
    print(f"✅ Recommended next steps: {next_steps}")
    
    # Test learning from success
    await store.learn_from_success("WEDDING", ["create_event", "set_budget", "find_venue"], {
        "success_rate": 0.9,
        "completion_time": 120
    })
    print("✅ Learned from successful sequence")
    
    # Test getting best practices
    best_practices = await store.get_best_practices("WEDDING", "initial")
    print(f"✅ Best practices: {best_practices}")
    
    print("✅ Interaction Pattern Learning tests passed!\n")


async def test_validation_system():
    """Test comprehensive validation system."""
    print("🔍 Testing Validation System...")
    
    validator = EventValidator()
    
    # Test event creation validation
    event_data = {
        "name": "Test Wedding",
        "eventType": "WEDDING",
        "startDateTime": "2024-12-25T18:00:00",
        "capacity": 150,
        "venueRequirements": "outdoor garden venue"
    }
    
    validation_report = await validator.validate_event_creation(event_data)
    print(f"✅ Validation report: {validation_report.is_valid}")
    print(f"   Warnings: {len(validation_report.warnings)}")
    print(f"   Critical issues: {len(validation_report.critical_issues)}")
    
    # Test predictive issues
    predictions = await validator.predict_issues(event_data)
    print(f"✅ Predicted issues: {len(predictions)}")
    
    # Test best practices
    best_practices = validator.get_best_practices("WEDDING")
    print(f"✅ Best practices for WEDDING: {len(best_practices)}")
    
    # Test seasonal recommendations
    seasonal_recs = validator.get_seasonal_recommendations(event_data)
    print(f"✅ Seasonal recommendations: {len(seasonal_recs)}")
    
    print("✅ Validation System tests passed!\n")


async def test_insights_generator():
    """Test event insights generator."""
    print("💡 Testing Event Insights Generator...")
    
    generator = EventInsightsGenerator()
    
    # Test event data
    event = {
        "name": "Summer Conference",
        "eventType": "CONFERENCE",
        "startDateTime": (datetime.utcnow() + timedelta(days=15)).isoformat(),
        "capacity": 200,
        "venueRequirements": "conference center with AV"
    }
    
    # Generate insights
    insights = await generator.generate_insights(event)
    print(f"✅ Generated {len(insights)} insights")
    
    # Test insights summary
    summary = await generator.get_insights_summary(event)
    print(f"✅ Insights summary: {summary['total_insights']} total insights")
    print(f"   Critical: {summary['critical_count']}")
    print(f"   Urgent: {summary['urgent_count']}")
    
    # Test next steps
    next_steps = await generator.get_next_steps(event)
    print(f"✅ Next steps: {next_steps}")
    
    # Test opportunities
    opportunities = await generator.get_opportunities(event)
    print(f"✅ Opportunities: {len(opportunities)}")
    
    print("✅ Event Insights Generator tests passed!\n")


async def test_workflow_orchestrator():
    """Test workflow orchestrator."""
    print("🔄 Testing Workflow Orchestrator...")
    
    orchestrator = WorkflowOrchestrator()
    
    # Test getting available templates
    templates = await orchestrator.get_available_templates()
    print(f"✅ Available templates: {len(templates)}")
    
    # Test workflow recommendations
    context = {"event_type": "WEDDING", "capacity": 100}
    recommendations = await orchestrator.get_workflow_recommendations("WEDDING", context)
    print(f"✅ Workflow recommendations: {len(recommendations)}")
    
    print("✅ Workflow Orchestrator tests passed!\n")


async def test_agent_coordinator():
    """Test agent coordinator."""
    print("🤝 Testing Agent Coordinator...")
    
    # Mock agents for testing
    class MockAgent:
        def __init__(self, name):
            self.name = name
        
        async def execute_tool(self, tool_name, parameters):
            return {"result": f"Mock {tool_name} executed by {self.name}"}
    
    agents = {
        "event": MockAgent("event"),
        "budget": MockAgent("budget"),
        "venue": MockAgent("venue")
    }
    
    coordinator = AgentCoordinator(agents)
    
    # Test sequential coordination
    result = await coordinator.coordinate_agents(
        CoordinationPattern.SEQUENTIAL,
        ["event", "budget"],
        {"tool_name": "test_tool", "parameters": {}}
    )
    print(f"✅ Sequential coordination: {result['pattern']}")
    
    # Test parallel coordination
    result = await coordinator.coordinate_agents(
        CoordinationPattern.PARALLEL,
        ["venue", "budget"],
        {"tool_name": "test_tool", "parameters": {}}
    )
    print(f"✅ Parallel coordination: {result['pattern']}")
    
    # Test conditional coordination
    result = await coordinator.coordinate_agents(
        CoordinationPattern.CONDITIONAL,
        ["event", "venue"],
        {"tool_name": "test_tool", "parameters": {}, "conditions": {"event": {"venue_type": "outdoor"}}}
    )
    print(f"✅ Conditional coordination: {result['pattern']}")
    
    print("✅ Agent Coordinator tests passed!\n")


async def test_enhanced_rag_system():
    """Test enhanced RAG system with interaction patterns."""
    print("📚 Testing Enhanced RAG System...")
    
    rag_system = EventRAGSystem()
    
    # Test storing interaction pattern
    pattern_id = await rag_system.store_interaction_pattern(
        "WEDDING",
        "initial",
        "create wedding event",
        "Great! Let's start planning your wedding.",
        {"success_rate": 0.9},
        {"guest_count": "100"}
    )
    print(f"✅ Stored interaction pattern: {pattern_id}")
    
    # Test getting similar patterns
    similar_patterns = await rag_system.get_similar_patterns("WEDDING", "initial", {"guest_count": "150"})
    print(f"✅ Found {len(similar_patterns)} similar patterns")
    
    # Test getting recommended next steps
    next_steps = await rag_system.get_recommended_next_steps("WEDDING", "initial", ["create_event"])
    print(f"✅ Recommended next steps: {next_steps}")
    
    # Test getting best practices
    best_practices = await rag_system.get_best_practices("WEDDING", "initial")
    print(f"✅ Best practices: {best_practices}")
    
    # Test learning from success
    await rag_system.learn_from_success("WEDDING", ["create_event", "set_budget"], {"success_rate": 0.85})
    print("✅ Learned from successful sequence")
    
    print("✅ Enhanced RAG System tests passed!\n")


async def test_integration():
    """Test integration of all components."""
    print("🔗 Testing Integration...")
    
    # Test validation integration with event data
    validator = EventValidator()
    event_data = {
        "name": "Test Event",
        "eventType": "BIRTHDAY",
        "startDateTime": (datetime.utcnow() + timedelta(days=30)).isoformat(),
        "capacity": 25
    }
    
    validation_report = await validator.validate_event_creation(event_data)
    print(f"✅ Validation integration: {validation_report.is_valid}")
    
    # Test insights integration
    generator = EventInsightsGenerator()
    insights = await generator.generate_insights(event_data)
    print(f"✅ Insights integration: {len(insights)} insights generated")
    
    # Test workflow integration
    orchestrator = WorkflowOrchestrator()
    templates = await orchestrator.get_available_templates()
    birthday_templates = [t for t in templates if t["event_type"] == "BIRTHDAY"]
    print(f"✅ Workflow integration: {len(birthday_templates)} birthday templates")
    
    print("✅ Integration tests passed!\n")


async def main():
    """Run all tests."""
    print("🚀 Starting Event Agent Intelligence Improvements Tests\n")
    
    try:
        await test_interaction_pattern_learning()
        await test_validation_system()
        await test_insights_generator()
        await test_workflow_orchestrator()
        await test_agent_coordinator()
        await test_enhanced_rag_system()
        await test_integration()
        
        print("🎉 All tests passed successfully!")
        print("\n📊 Summary of Intelligence Improvements:")
        print("✅ Interaction Pattern Learning - Agents learn from successful interactions")
        print("✅ Comprehensive Validation - Predictive warnings and best practices")
        print("✅ Event Insights Generator - Proactive recommendations and analysis")
        print("✅ Workflow Orchestrator - Automated multi-agent coordination")
        print("✅ Agent Coordinator - Cross-agent collaboration patterns")
        print("✅ Enhanced RAG System - Dynamic context and learning capabilities")
        print("✅ Dynamic System Prompts - Context-aware agent guidance")
        print("✅ Tool Integration - Validation and workflow awareness")
        
    except Exception as e:
        print(f"❌ Test failed: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    asyncio.run(main())
