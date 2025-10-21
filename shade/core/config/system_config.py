"""System configuration management."""

from typing import Dict, Any, List, Optional
from dataclasses import dataclass
from enum import Enum


class LogLevel(Enum):
    """Logging levels."""
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARNING = "WARNING"
    ERROR = "ERROR"
    CRITICAL = "CRITICAL"


class DatabaseType(Enum):
    """Database types."""
    MONGODB = "mongodb"
    POSTGRESQL = "postgresql"
    REDIS = "redis"


@dataclass
class SystemConfig:
    """System-wide configuration."""
    # Application settings
    app_name: str = "Event Planner AI"
    version: str = "1.0.0"
    debug: bool = False
    log_level: LogLevel = LogLevel.INFO
    
    # Agent settings
    max_concurrent_agents: int = 10
    agent_timeout: int = 300
    max_retry_attempts: int = 3
    
    # Database settings
    database_type: DatabaseType = DatabaseType.MONGODB
    database_url: str = "mongodb://localhost:27017"
    database_name: str = "event_planner"
    
    # Redis settings
    redis_url: str = "redis://localhost:6379"
    redis_db: int = 0
    
    # External API settings
    openai_api_key: str = ""
    google_api_key: str = ""
    weather_api_key: str = ""
    
    # Workflow settings
    max_workflow_steps: int = 50
    workflow_timeout: int = 1800  # 30 minutes
    
    # Learning settings
    enable_learning: bool = True
    learning_retention_days: int = 90
    pattern_similarity_threshold: float = 0.7
    
    # Validation settings
    enable_validation: bool = True
    validation_strict_mode: bool = False
    
    # Communication settings
    message_bus_enabled: bool = True
    max_message_queue_size: int = 1000
    
    # Performance settings
    enable_caching: bool = True
    cache_ttl: int = 3600  # 1 hour
    max_cache_size: int = 10000


class SystemConfigManager:
    """Manages system configuration."""
    
    def __init__(self):
        """Initialize the system config manager."""
        self.config = SystemConfig()
        self._load_from_environment()
    
    def _load_from_environment(self):
        """Load configuration from environment variables."""
        import os
        
        # Load from environment variables
        self.config.debug = os.getenv("DEBUG", "false").lower() == "true"
        self.config.log_level = LogLevel(os.getenv("LOG_LEVEL", "INFO"))
        
        # Database settings
        self.config.database_url = os.getenv("DATABASE_URL", self.config.database_url)
        self.config.database_name = os.getenv("DATABASE_NAME", self.config.database_name)
        
        # Redis settings
        self.config.redis_url = os.getenv("REDIS_URL", self.config.redis_url)
        
        # API keys
        self.config.openai_api_key = os.getenv("OPENAI_API_KEY", self.config.openai_api_key)
        self.config.google_api_key = os.getenv("GOOGLE_API_KEY", self.config.google_api_key)
        self.config.weather_api_key = os.getenv("WEATHER_API_KEY", self.config.weather_api_key)
        
        # Performance settings
        self.config.max_concurrent_agents = int(os.getenv("MAX_CONCURRENT_AGENTS", self.config.max_concurrent_agents))
        self.config.agent_timeout = int(os.getenv("AGENT_TIMEOUT", self.config.agent_timeout))
    
    def get_config(self) -> SystemConfig:
        """Get the current system configuration."""
        return self.config
    
    def update_config(self, updates: Dict[str, Any]) -> None:
        """Update system configuration."""
        for key, value in updates.items():
            if hasattr(self.config, key):
                setattr(self.config, key, value)
    
    def get_database_config(self) -> Dict[str, Any]:
        """Get database configuration."""
        return {
            "type": self.config.database_type.value,
            "url": self.config.database_url,
            "name": self.config.database_name
        }
    
    def get_redis_config(self) -> Dict[str, Any]:
        """Get Redis configuration."""
        return {
            "url": self.config.redis_url,
            "db": self.config.redis_db
        }
    
    def get_api_keys(self) -> Dict[str, str]:
        """Get API keys."""
        return {
            "openai": self.config.openai_api_key,
            "google": self.config.google_api_key,
            "weather": self.config.weather_api_key
        }
    
    def is_development(self) -> bool:
        """Check if running in development mode."""
        return self.config.debug
    
    def is_production(self) -> bool:
        """Check if running in production mode."""
        return not self.config.debug
