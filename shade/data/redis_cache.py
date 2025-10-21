"""Redis cache service for high-performance caching."""

import os
import asyncio
import logging
import json
import pickle
from typing import Dict, Any, List, Optional, Union
from datetime import datetime, timedelta
import redis.asyncio as redis
from redis.exceptions import ConnectionError, TimeoutError

logger = logging.getLogger(__name__)


class RedisCache:
    """Redis cache service with fallback to in-memory cache."""
    
    def __init__(self):
        """Initialize Redis cache service."""
        self.redis_client = None
        self.connected = False
        self.redis_url = os.getenv("REDIS_URL", "redis://localhost:6379")
        self.memory_cache = {}
        self.default_ttl = 3600  # 1 hour
        
    async def connect(self):
        """Connect to Redis with fallback to memory cache."""
        try:
            self.redis_client = redis.from_url(
                self.redis_url,
                decode_responses=True,
                socket_connect_timeout=5,
                socket_timeout=5,
                retry_on_timeout=True
            )
            
            # Test connection
            await self.redis_client.ping()
            self.connected = True
            
            logger.info("Connected to Redis cache")
            return True
            
        except (ConnectionError, TimeoutError) as e:
            logger.warning(f"Redis not available: {e}. Using in-memory cache.")
            self.connected = False
            return False
        except Exception as e:
            logger.error(f"Error connecting to Redis: {e}")
            self.connected = False
            return False
    
    async def get(self, key: str) -> Optional[Any]:
        """Get value from cache."""
        try:
            if self.connected and self.redis_client:
                value = await self.redis_client.get(key)
                if value:
                    try:
                        return json.loads(value)
                    except json.JSONDecodeError:
                        return value
                return None
            else:
                # Fallback to memory cache
                if key in self.memory_cache:
                    cache_item = self.memory_cache[key]
                    if cache_item["expires_at"] > datetime.utcnow():
                        return cache_item["value"]
                    else:
                        del self.memory_cache[key]
                return None
                
        except Exception as e:
            logger.error(f"Error getting cache key {key}: {e}")
            return None
    
    async def set(self, key: str, value: Any, ttl: int = None) -> bool:
        """Set value in cache with TTL."""
        try:
            ttl = ttl or self.default_ttl
            
            if self.connected and self.redis_client:
                if isinstance(value, (dict, list)):
                    value = json.dumps(value)
                await self.redis_client.setex(key, ttl, value)
                return True
            else:
                # Fallback to memory cache
                self.memory_cache[key] = {
                    "value": value,
                    "expires_at": datetime.utcnow() + timedelta(seconds=ttl)
                }
                return True
                
        except Exception as e:
            logger.error(f"Error setting cache key {key}: {e}")
            return False
    
    async def delete(self, key: str) -> bool:
        """Delete key from cache."""
        try:
            if self.connected and self.redis_client:
                result = await self.redis_client.delete(key)
                return result > 0
            else:
                # Fallback to memory cache
                if key in self.memory_cache:
                    del self.memory_cache[key]
                    return True
                return False
                
        except Exception as e:
            logger.error(f"Error deleting cache key {key}: {e}")
            return False
    
    async def exists(self, key: str) -> bool:
        """Check if key exists in cache."""
        try:
            if self.connected and self.redis_client:
                result = await self.redis_client.exists(key)
                return result > 0
            else:
                # Fallback to memory cache
                if key in self.memory_cache:
                    cache_item = self.memory_cache[key]
                    if cache_item["expires_at"] > datetime.utcnow():
                        return True
                    else:
                        del self.memory_cache[key]
                return False
                
        except Exception as e:
            logger.error(f"Error checking cache key {key}: {e}")
            return False
    
    async def expire(self, key: str, ttl: int) -> bool:
        """Set expiration for key."""
        try:
            if self.connected and self.redis_client:
                result = await self.redis_client.expire(key, ttl)
                return result
            else:
                # Fallback to memory cache
                if key in self.memory_cache:
                    self.memory_cache[key]["expires_at"] = datetime.utcnow() + timedelta(seconds=ttl)
                    return True
                return False
                
        except Exception as e:
            logger.error(f"Error setting expiration for key {key}: {e}")
            return False
    
    async def get_many(self, keys: List[str]) -> Dict[str, Any]:
        """Get multiple keys from cache."""
        try:
            if self.connected and self.redis_client:
                values = await self.redis_client.mget(keys)
                result = {}
                for key, value in zip(keys, values):
                    if value:
                        try:
                            result[key] = json.loads(value)
                        except json.JSONDecodeError:
                            result[key] = value
                return result
            else:
                # Fallback to memory cache
                result = {}
                for key in keys:
                    value = await self.get(key)
                    if value is not None:
                        result[key] = value
                return result
                
        except Exception as e:
            logger.error(f"Error getting multiple cache keys: {e}")
            return {}
    
    async def set_many(self, mapping: Dict[str, Any], ttl: int = None) -> bool:
        """Set multiple keys in cache."""
        try:
            ttl = ttl or self.default_ttl
            
            if self.connected and self.redis_client:
                # Prepare values for Redis
                redis_mapping = {}
                for key, value in mapping.items():
                    if isinstance(value, (dict, list)):
                        redis_mapping[key] = json.dumps(value)
                    else:
                        redis_mapping[key] = value
                
                await self.redis_client.mset(redis_mapping)
                
                # Set expiration for all keys
                if ttl > 0:
                    for key in mapping.keys():
                        await self.redis_client.expire(key, ttl)
                
                return True
            else:
                # Fallback to memory cache
                for key, value in mapping.items():
                    await self.set(key, value, ttl)
                return True
                
        except Exception as e:
            logger.error(f"Error setting multiple cache keys: {e}")
            return False
    
    async def increment(self, key: str, amount: int = 1) -> int:
        """Increment a numeric value in cache."""
        try:
            if self.connected and self.redis_client:
                result = await self.redis_client.incrby(key, amount)
                return result
            else:
                # Fallback to memory cache
                current_value = await self.get(key)
                if current_value is None:
                    new_value = amount
                else:
                    new_value = int(current_value) + amount
                await self.set(key, new_value)
                return new_value
                
        except Exception as e:
            logger.error(f"Error incrementing cache key {key}: {e}")
            return 0
    
    async def decrement(self, key: str, amount: int = 1) -> int:
        """Decrement a numeric value in cache."""
        try:
            if self.connected and self.redis_client:
                result = await self.redis_client.decrby(key, amount)
                return result
            else:
                # Fallback to memory cache
                current_value = await self.get(key)
                if current_value is None:
                    new_value = -amount
                else:
                    new_value = int(current_value) - amount
                await self.set(key, new_value)
                return new_value
                
        except Exception as e:
            logger.error(f"Error decrementing cache key {key}: {e}")
            return 0
    
    # Specialized cache methods for different data types
    async def cache_conversation(self, user_id: str, chat_id: str, conversation: Dict[str, Any], ttl: int = 3600) -> bool:
        """Cache conversation data."""
        key = f"conversation:{user_id}:{chat_id}"
        return await self.set(key, conversation, ttl)
    
    async def get_cached_conversation(self, user_id: str, chat_id: str) -> Optional[Dict[str, Any]]:
        """Get cached conversation data."""
        key = f"conversation:{user_id}:{chat_id}"
        return await self.get(key)
    
    async def cache_agent_response(self, agent_name: str, query: str, response: Dict[str, Any], ttl: int = 1800) -> bool:
        """Cache agent response."""
        key = f"agent_response:{agent_name}:{hash(query)}"
        return await self.set(key, response, ttl)
    
    async def get_cached_agent_response(self, agent_name: str, query: str) -> Optional[Dict[str, Any]]:
        """Get cached agent response."""
        key = f"agent_response:{agent_name}:{hash(query)}"
        return await self.get(key)
    
    async def cache_weather_data(self, location: str, weather_data: Dict[str, Any], ttl: int = 900) -> bool:
        """Cache weather data (15 minutes TTL)."""
        key = f"weather:{location}"
        return await self.set(key, weather_data, ttl)
    
    async def get_cached_weather_data(self, location: str) -> Optional[Dict[str, Any]]:
        """Get cached weather data."""
        key = f"weather:{location}"
        return await self.get(key)
    
    async def cache_search_results(self, query: str, results: List[Dict[str, Any]], ttl: int = 1800) -> bool:
        """Cache search results."""
        key = f"search:{hash(query)}"
        return await self.set(key, results, ttl)
    
    async def get_cached_search_results(self, query: str) -> Optional[List[Dict[str, Any]]]:
        """Get cached search results."""
        key = f"search:{hash(query)}"
        return await self.get(key)
    
    async def clear_pattern(self, pattern: str) -> int:
        """Clear keys matching a pattern."""
        try:
            if self.connected and self.redis_client:
                keys = await self.redis_client.keys(pattern)
                if keys:
                    result = await self.redis_client.delete(*keys)
                    return result
                return 0
            else:
                # Fallback to memory cache
                keys_to_delete = []
                for key in self.memory_cache.keys():
                    if pattern.replace("*", "") in key:
                        keys_to_delete.append(key)
                
                for key in keys_to_delete:
                    del self.memory_cache[key]
                
                return len(keys_to_delete)
                
        except Exception as e:
            logger.error(f"Error clearing cache pattern {pattern}: {e}")
            return 0
    
    async def get_cache_stats(self) -> Dict[str, Any]:
        """Get cache statistics."""
        try:
            if self.connected and self.redis_client:
                info = await self.redis_client.info()
                return {
                    "connected": True,
                    "used_memory": info.get("used_memory_human", "0B"),
                    "connected_clients": info.get("connected_clients", 0),
                    "total_commands_processed": info.get("total_commands_processed", 0),
                    "keyspace_hits": info.get("keyspace_hits", 0),
                    "keyspace_misses": info.get("keyspace_misses", 0)
                }
            else:
                return {
                    "connected": False,
                    "memory_cache_size": len(self.memory_cache),
                    "mode": "memory_only"
                }
                
        except Exception as e:
            logger.error(f"Error getting cache stats: {e}")
            return {"error": str(e)}
    
    async def cleanup_expired(self) -> int:
        """Clean up expired items from memory cache."""
        try:
            if not self.connected:
                current_time = datetime.utcnow()
                keys_to_delete = []
                
                for key, cache_item in self.memory_cache.items():
                    if cache_item["expires_at"] <= current_time:
                        keys_to_delete.append(key)
                
                for key in keys_to_delete:
                    del self.memory_cache[key]
                
                logger.info(f"Cleaned up {len(keys_to_delete)} expired cache items")
                return len(keys_to_delete)
            
            return 0
            
        except Exception as e:
            logger.error(f"Error cleaning up expired cache items: {e}")
            return 0
    
    async def disconnect(self):
        """Disconnect from Redis."""
        try:
            if self.redis_client:
                await self.redis_client.close()
                self.connected = False
                logger.info("Disconnected from Redis")
        except Exception as e:
            logger.error(f"Error disconnecting from Redis: {e}")
