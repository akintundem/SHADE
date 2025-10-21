"""Interaction pattern storage system for learning from successful event planning sessions."""

from typing import Dict, Any, List, Optional
import asyncio
import logging
import json
from datetime import datetime, timedelta
from dataclasses import dataclass, asdict

logger = logging.getLogger(__name__)


@dataclass
class InteractionPattern:
    """Represents a successful interaction pattern."""
    event_type: str
    planning_phase: str
    user_action: str
    agent_response: str
    success_metrics: Dict[str, Any]
    context: Dict[str, Any]
    timestamp: str
    pattern_id: str


@dataclass
class PlanningSequence:
    """Represents a sequence of successful planning steps."""
    event_type: str
    sequence: List[str]  # List of action types
    success_rate: float
    average_completion_time: int  # minutes
    common_issues: List[str]
    best_practices: List[str]


class InteractionPatternStore:
    """Stores and retrieves interaction patterns for learning."""
    
    def __init__(self):
        """Initialize the interaction pattern store."""
        self.patterns: List[InteractionPattern] = []
        self.sequences: List[PlanningSequence] = []
        self._lock = asyncio.Lock()
        
        # Initialize with some common patterns
        self._initialize_common_patterns()
    
    def _initialize_common_patterns(self):
        """Initialize with common successful patterns."""
        # Wedding planning patterns
        wedding_patterns = [
            InteractionPattern(
                event_type="WEDDING",
                planning_phase="initial",
                user_action="create wedding event",
                agent_response="Great! Let's start with your guest count and budget to find the perfect venue.",
                success_metrics={"completion_rate": 0.85, "user_satisfaction": 4.2},
                context={"guest_count": "100-200", "budget_range": "$20k-50k"},
                timestamp=datetime.utcnow().isoformat(),
                pattern_id="wedding_initial_001"
            ),
            InteractionPattern(
                event_type="WEDDING",
                planning_phase="venue_selection",
                user_action="search venues",
                agent_response="I'll help you find venues that match your style and budget. What's your preferred location?",
                success_metrics={"venue_found_rate": 0.92, "user_satisfaction": 4.5},
                context={"location_preference": "downtown", "style": "modern"},
                timestamp=datetime.utcnow().isoformat(),
                pattern_id="wedding_venue_001"
            )
        ]
        
        # Corporate event patterns
        corporate_patterns = [
            InteractionPattern(
                event_type="CONFERENCE",
                planning_phase="initial",
                user_action="create conference event",
                agent_response="Perfect! For corporate events, let's start with capacity and technical requirements.",
                success_metrics={"completion_rate": 0.78, "user_satisfaction": 4.0},
                context={"capacity": "50-500", "tech_requirements": "AV, WiFi"},
                timestamp=datetime.utcnow().isoformat(),
                pattern_id="corporate_initial_001"
            )
        ]
        
        # Birthday party patterns
        birthday_patterns = [
            InteractionPattern(
                event_type="BIRTHDAY",
                planning_phase="initial",
                user_action="create birthday party",
                agent_response="Exciting! Let's start with the age group and theme to make it special.",
                success_metrics={"completion_rate": 0.90, "user_satisfaction": 4.6},
                context={"age_group": "children", "theme": "superhero"},
                timestamp=datetime.utcnow().isoformat(),
                pattern_id="birthday_initial_001"
            )
        ]
        
        self.patterns.extend(wedding_patterns + corporate_patterns + birthday_patterns)
        
        # Initialize common sequences
        self.sequences = [
            PlanningSequence(
                event_type="WEDDING",
                sequence=["create_event", "set_budget", "find_venue", "select_vendors", "create_timeline"],
                success_rate=0.88,
                average_completion_time=120,
                common_issues=["venue_availability", "budget_overruns"],
                best_practices=["book_venue_early", "get_multiple_quotes"]
            ),
            PlanningSequence(
                event_type="CONFERENCE",
                sequence=["create_event", "find_venue", "setup_tech", "invite_attendees", "create_agenda"],
                success_rate=0.82,
                average_completion_time=90,
                common_issues=["tech_requirements", "catering_dietary"],
                best_practices=["test_tech_early", "confirm_dietary_restrictions"]
            ),
            PlanningSequence(
                event_type="BIRTHDAY",
                sequence=["create_event", "choose_theme", "find_venue", "book_entertainment", "send_invitations"],
                success_rate=0.94,
                average_completion_time=60,
                common_issues=["entertainment_availability", "age_appropriate_activities"],
                best_practices=["book_entertainment_early", "plan_age_appropriate_activities"]
            )
        ]
    
    async def store_interaction_pattern(self, pattern: InteractionPattern) -> str:
        """Store a new interaction pattern."""
        async with self._lock:
            pattern.pattern_id = f"{pattern.event_type.lower()}_{len(self.patterns)}_{datetime.utcnow().timestamp()}"
            self.patterns.append(pattern)
            logger.info(f"Stored interaction pattern: {pattern.pattern_id}")
            return pattern.pattern_id
    
    async def get_similar_patterns(self, event_type: str, planning_phase: str, context: Dict[str, Any], limit: int = 5) -> List[InteractionPattern]:
        """Get similar patterns based on event type, phase, and context."""
        async with self._lock:
            similar_patterns = []
            
            for pattern in self.patterns:
                if pattern.event_type == event_type and pattern.planning_phase == planning_phase:
                    # Calculate similarity based on context overlap
                    similarity_score = self._calculate_similarity(pattern.context, context)
                    if similarity_score > 0.3:  # Threshold for similarity
                        similar_patterns.append((pattern, similarity_score))
            
            # Sort by similarity score and return top results
            similar_patterns.sort(key=lambda x: x[1], reverse=True)
            return [pattern for pattern, _ in similar_patterns[:limit]]
    
    def _calculate_similarity(self, context1: Dict[str, Any], context2: Dict[str, Any]) -> float:
        """Calculate similarity between two contexts."""
        if not context1 or not context2:
            return 0.0
        
        # Simple overlap calculation
        keys1 = set(context1.keys())
        keys2 = set(context2.keys())
        
        if not keys1 or not keys2:
            return 0.0
        
        overlap = len(keys1.intersection(keys2))
        total = len(keys1.union(keys2))
        
        return overlap / total if total > 0 else 0.0
    
    async def learn_from_success(self, event_type: str, completed_sequence: List[str], success_metrics: Dict[str, Any]) -> None:
        """Learn from a successful planning sequence."""
        async with self._lock:
            # Find existing sequence or create new one
            existing_sequence = None
            for seq in self.sequences:
                if seq.event_type == event_type and seq.sequence == completed_sequence:
                    existing_sequence = seq
                    break
            
            if existing_sequence:
                # Update existing sequence with new success data
                existing_sequence.success_rate = (existing_sequence.success_rate + success_metrics.get("success_rate", 0.8)) / 2
                existing_sequence.average_completion_time = (existing_sequence.average_completion_time + success_metrics.get("completion_time", 60)) / 2
            else:
                # Create new sequence
                new_sequence = PlanningSequence(
                    event_type=event_type,
                    sequence=completed_sequence,
                    success_rate=success_metrics.get("success_rate", 0.8),
                    average_completion_time=success_metrics.get("completion_time", 60),
                    common_issues=success_metrics.get("common_issues", []),
                    best_practices=success_metrics.get("best_practices", [])
                )
                self.sequences.append(new_sequence)
            
            logger.info(f"Updated learning for {event_type} sequence")
    
    async def get_recommended_next_steps(self, event_type: str, current_phase: str, completed_actions: List[str]) -> List[str]:
        """Get recommended next steps based on successful patterns."""
        async with self._lock:
            # Find matching sequence
            for sequence in self.sequences:
                if sequence.event_type == event_type:
                    # Find where we are in the sequence
                    try:
                        current_index = sequence.sequence.index(completed_actions[-1]) if completed_actions else 0
                        if current_index < len(sequence.sequence) - 1:
                            return sequence.sequence[current_index + 1:current_index + 3]  # Next 2 steps
                    except ValueError:
                        # Current action not in sequence, return first few steps
                        return sequence.sequence[:2]
            
            # Default recommendations based on event type
            default_recommendations = {
                "WEDDING": ["set_budget", "find_venue"],
                "CONFERENCE": ["find_venue", "setup_tech"],
                "BIRTHDAY": ["choose_theme", "find_venue"],
                "PARTY": ["choose_theme", "find_venue"]
            }
            
            return default_recommendations.get(event_type, ["set_budget", "find_venue"])
    
    async def get_best_practices(self, event_type: str, planning_phase: str) -> List[str]:
        """Get best practices for a specific event type and phase."""
        async with self._lock:
            practices = []
            
            # Get practices from sequences
            for sequence in self.sequences:
                if sequence.event_type == event_type:
                    practices.extend(sequence.best_practices)
            
            # Get practices from patterns
            for pattern in self.patterns:
                if pattern.event_type == event_type and pattern.planning_phase == planning_phase:
                    if "best_practices" in pattern.context:
                        practices.extend(pattern.context["best_practices"])
            
            # Remove duplicates and return
            return list(set(practices))
    
    async def get_pattern_stats(self) -> Dict[str, Any]:
        """Get statistics about stored patterns."""
        async with self._lock:
            event_type_counts = {}
            phase_counts = {}
            
            for pattern in self.patterns:
                event_type_counts[pattern.event_type] = event_type_counts.get(pattern.event_type, 0) + 1
                phase_counts[pattern.planning_phase] = phase_counts.get(pattern.planning_phase, 0) + 1
            
            return {
                "total_patterns": len(self.patterns),
                "total_sequences": len(self.sequences),
                "event_type_distribution": event_type_counts,
                "phase_distribution": phase_counts,
                "average_success_rate": sum(seq.success_rate for seq in self.sequences) / len(self.sequences) if self.sequences else 0
            }
    
    async def cleanup_old_patterns(self, max_age_days: int = 30) -> int:
        """Clean up old patterns."""
        cutoff_time = datetime.utcnow() - timedelta(days=max_age_days)
        cleaned_count = 0
        
        async with self._lock:
            patterns_to_keep = []
            for pattern in self.patterns:
                pattern_time = datetime.fromisoformat(pattern.timestamp)
                if pattern_time > cutoff_time:
                    patterns_to_keep.append(pattern)
                else:
                    cleaned_count += 1
            
            self.patterns = patterns_to_keep
        
        return cleaned_count
