"""Template for creating new agents quickly."""

from typing import Dict, Any, List, Optional
import os
import logging

logger = logging.getLogger(__name__)


class AgentTemplate:
    """Template for creating new agents."""
    
    def __init__(self, agent_type: str, agent_name: str, description: str):
        """Initialize the agent template."""
        self.agent_type = agent_type
        self.agent_name = agent_name
        self.description = description
        self.tools = []
        self.rag_system = None
        self.capabilities = []
    
    def add_tool(self, tool_name: str, tool_function: str, description: str):
        """Add a tool to the agent template."""
        self.tools.append({
            "name": tool_name,
            "function": tool_function,
            "description": description
        })
    
    def set_rag_system(self, rag_system: str):
        """Set the RAG system for the agent."""
        self.rag_system = rag_system
    
    def add_capability(self, capability: str):
        """Add a capability to the agent."""
        self.capabilities.append(capability)
    
    def generate_agent_code(self) -> str:
        """Generate agent code from template."""
        return f'''"""Generated {self.agent_name} agent."""

from typing import Dict, Any, List
from ..core.base_agent import BaseAgent
from .tools.{self.agent_type}_tools import {', '.join([tool['function'] for tool in self.tools])}
from .rag.{self.agent_type}_rag import {self.rag_system or f'{self.agent_type.title()}RAGSystem'}

class {self.agent_name.replace(' ', '')}Agent(BaseAgent):
    """{self.description}"""
    
    def __init__(self, message_bus=None, config=None):
        super().__init__("{self.agent_name}", "gpt-4o", 0.7, message_bus, config)
    
    def get_system_prompt(self, context: Dict[str, Any] = None) -> str:
        """Get the system prompt for the {self.agent_name}."""
        base_prompt = """
        You are a {self.description.lower()}.
        
        **YOUR CAPABILITIES:**
        {chr(10).join([f"• {cap}" for cap in self.capabilities])}
        
        **YOUR TOOLS:**
        {chr(10).join([f"• {tool['name']}: {tool['description']}" for tool in self.tools])}
        
        **CONVERSATION STYLE:**
        - Be professional and helpful
        - Ask clarifying questions when needed
        - Provide clear explanations
        - Suggest next steps when appropriate
        
        **RULES:**
        - Always use the appropriate tools for tasks
        - Provide accurate information
        - Be proactive in suggesting improvements
        - Maintain context throughout the conversation
        """
        
        # Add dynamic context if available
        if context:
            dynamic_context = self._build_dynamic_context(context)
            if dynamic_context:
                base_prompt += f"\\n\\n**CURRENT CONTEXT:**\\n{{dynamic_context}}"
        
        return base_prompt
    
    def _build_dynamic_context(self, context: Dict[str, Any]) -> str:
        """Build dynamic context for the system prompt."""
        context_parts = []
        
        # Add context-specific information
        if context.get("current_task"):
            context_parts.append(f"**CURRENT TASK:** {context['current_task']}")
        
        if context.get("user_preferences"):
            context_parts.append(f"**USER PREFERENCES:** {context['user_preferences']}")
        
        if context.get("constraints"):
            context_parts.append(f"**CONSTRAINTS:** {context['constraints']}")
        
        return "\\n\\n".join(context_parts)
    
    def get_tools(self) -> List[Any]:
        """Get tools available to the {self.agent_name}."""
        return [
            {', '.join([tool['function'] for tool in self.tools])}
        ]
    
    def get_rag_system(self):
        """Get the RAG system for the {self.agent_name}."""
        return {self.rag_system or f'{self.agent_type.title()}RAGSystem'}()
'''
    
    def generate_tools_code(self) -> str:
        """Generate tools code from template."""
        return f'''"""Generated {self.agent_type} tools."""

from langchain.tools import tool
from typing import Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)

{chr(10).join([self._generate_tool_code(tool) for tool in self.tools])}
'''
    
    def _generate_tool_code(self, tool: Dict[str, str]) -> str:
        """Generate code for a single tool."""
        return f'''
@tool
async def {tool['function']}(
    # Add parameters as needed
    parameter: str
) -> Dict[str, Any]:
    """
    {tool['description']}
    
    Args:
        parameter: Description of parameter
    
    Returns:
        Dict with result
    """
    try:
        # Tool implementation
        return {{
            "success": True,
            "result": "Tool executed successfully",
            "message": f"{tool['name']} completed"
        }}
    except Exception as e:
        logger.error(f"Error in {tool['function']}: {{e}}")
        return {{
            "success": False,
            "error": str(e),
            "message": f"Error executing {tool['name']}"
        }}
'''
    
    def generate_rag_code(self) -> str:
        """Generate RAG system code from template."""
        return f'''"""Generated {self.agent_type} RAG system."""

from typing import Dict, Any, List
from ...core.base_rag import BaseRAGSystem

class {self.rag_system or f'{self.agent_type.title()}RAGSystem'}(BaseRAGSystem):
    """RAG system for {self.agent_type} knowledge."""
    
    def __init__(self):
        super().__init__("{self.agent_type}")
        self._initialize_knowledge_base()
    
    def _initialize_knowledge_base(self):
        """Initialize with {self.agent_type} knowledge."""
        self.knowledge_base = [
            {{
                "content": "{self.agent_type.title()} requires careful planning and execution.",
                "metadata": {{"type": "general", "category": "planning"}},
                "domain": "{self.agent_type}"
            }},
            {{
                "content": "Key {self.agent_type} phases include planning, execution, and evaluation.",
                "metadata": {{"type": "process", "category": "methodology"}},
                "domain": "{self.agent_type}"
            }}
        ]
    
    async def load_knowledge_base(self):
        """Load {self.agent_type} knowledge base."""
        pass
    
    async def get_relevant_context(self, query: str) -> str:
        """Get relevant {self.agent_type} context."""
        try:
            relevant_items = await self.search_knowledge(query, limit=3)
            
            if not relevant_items:
                return "No specific {self.agent_type} context found."
            
            context_parts = []
            for item in relevant_items:
                context_parts.append(f"- {{item['content']}}")
            
            return f"{self.agent_type.title()} Context:\\n" + "\\n".join(context_parts)
            
        except Exception as e:
            return f"Error retrieving {self.agent_type} context: {{str(e)}}"
'''
    
    def create_agent_directory(self, base_path: str) -> bool:
        """Create agent directory structure."""
        try:
            agent_path = os.path.join(base_path, self.agent_type)
            
            # Create directories
            os.makedirs(agent_path, exist_ok=True)
            os.makedirs(os.path.join(agent_path, "tools"), exist_ok=True)
            os.makedirs(os.path.join(agent_path, "rag"), exist_ok=True)
            
            # Create __init__.py files
            with open(os.path.join(agent_path, "__init__.py"), "w") as f:
                f.write(f'"""Generated {self.agent_type} agent."""\\n\\nfrom .{self.agent_type}_agent import {self.agent_name.replace(" ", "")}Agent\\n\\n__all__ = ["{self.agent_name.replace(" ", "")}Agent"]')
            
            with open(os.path.join(agent_path, "tools", "__init__.py"), "w") as f:
                f.write(f'"""Generated {self.agent_type} tools."""\\n\\nfrom .{self.agent_type}_tools import {", ".join([tool["function"] for tool in self.tools])}\\n\\n__all__ = {[tool["function"] for tool in self.tools]}')
            
            with open(os.path.join(agent_path, "rag", "__init__.py"), "w") as f:
                f.write(f'"""Generated {self.agent_type} RAG system."""\\n\\nfrom .{self.agent_type}_rag import {self.rag_system or f"{self.agent_type.title()}RAGSystem"}\\n\\n__all__ = ["{self.rag_system or f"{self.agent_type.title()}RAGSystem"}"]')
            
            # Create agent file
            with open(os.path.join(agent_path, f"{self.agent_type}_agent.py"), "w") as f:
                f.write(self.generate_agent_code())
            
            # Create tools file
            with open(os.path.join(agent_path, "tools", f"{self.agent_type}_tools.py"), "w") as f:
                f.write(self.generate_tools_code())
            
            # Create RAG file
            with open(os.path.join(agent_path, "rag", f"{self.agent_type}_rag.py"), "w") as f:
                f.write(self.generate_rag_code())
            
            logger.info(f"Created agent directory structure for {self.agent_type}")
            return True
            
        except Exception as e:
            logger.error(f"Error creating agent directory: {e}")
            return False
    
    def get_template_info(self) -> Dict[str, Any]:
        """Get template information."""
        return {
            "agent_type": self.agent_type,
            "agent_name": self.agent_name,
            "description": self.description,
            "tools": self.tools,
            "rag_system": self.rag_system,
            "capabilities": self.capabilities
        }
