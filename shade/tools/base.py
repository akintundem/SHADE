"""Base tool wrapper for Java backend API calls."""

import httpx
import os
from typing import Dict, Any, Optional
from dotenv import load_dotenv

load_dotenv()


class JavaAPIClient:
    """HTTP client for Java backend API calls."""
    
    def __init__(self):
        self.base_url = os.getenv("JAVA_BACKEND_URL", "http://localhost:8080")
        self.timeout = 30.0
    
    async def get(self, endpoint: str, params: Optional[Dict] = None) -> Dict[str, Any]:
        """Make GET request to Java backend."""
        url = f"{self.base_url}{endpoint}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.get(url, params=params)
                response.raise_for_status()
                return response.json()
            except httpx.HTTPError as e:
                return {
                    "success": False,
                    "error": f"HTTP error: {str(e)}",
                    "status_code": getattr(e.response, 'status_code', None)
                }
            except Exception as e:
                return {
                    "success": False,
                    "error": f"Request error: {str(e)}"
                }
    
    async def post(self, endpoint: str, data: Dict[str, Any]) -> Dict[str, Any]:
        """Make POST request to Java backend."""
        url = f"{self.base_url}{endpoint}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.post(url, json=data)
                response.raise_for_status()
                return response.json()
            except httpx.HTTPError as e:
                return {
                    "success": False,
                    "error": f"HTTP error: {str(e)}",
                    "status_code": getattr(e.response, 'status_code', None)
                }
            except Exception as e:
                return {
                    "success": False,
                    "error": f"Request error: {str(e)}"
                }
    
    async def put(self, endpoint: str, data: Dict[str, Any]) -> Dict[str, Any]:
        """Make PUT request to Java backend."""
        url = f"{self.base_url}{endpoint}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.put(url, json=data)
                response.raise_for_status()
                return response.json()
            except httpx.HTTPError as e:
                return {
                    "success": False,
                    "error": f"HTTP error: {str(e)}",
                    "status_code": getattr(e.response, 'status_code', None)
                }
            except Exception as e:
                return {
                    "success": False,
                    "error": f"Request error: {str(e)}"
                }
    
    async def delete(self, endpoint: str) -> Dict[str, Any]:
        """Make DELETE request to Java backend."""
        url = f"{self.base_url}{endpoint}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            try:
                response = await client.delete(url)
                response.raise_for_status()
                return {"success": True, "message": "Deleted successfully"}
            except httpx.HTTPError as e:
                return {
                    "success": False,
                    "error": f"HTTP error: {str(e)}",
                    "status_code": getattr(e.response, 'status_code', None)
                }
            except Exception as e:
                return {
                    "success": False,
                    "error": f"Request error: {str(e)}"
                }


# Global client instance
java_client = JavaAPIClient()
