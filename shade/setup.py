#!/usr/bin/env python3
"""Setup script to create .env file for the shade application."""

import os

def create_env_file():
    """Create a .env file with template values."""
    env_content = """# AI Services
OPENAI_API_KEY=your-openai-api-key

# Note: Weather tool uses mock data, so OPENWEATHER_API_KEY is optional
# OPENWEATHER_API_KEY=your-openweather-api-key
"""
    
    env_path = ".env"
    
    if os.path.exists(env_path):
        print(f"⚠️  {env_path} already exists. Skipping creation.")
        return
    
    try:
        with open(env_path, 'w') as f:
            f.write(env_content)
        print(f"✅ Created {env_path} with template values.")
        print("📝 Please edit the file and add your actual API key:")
        print("   - Get OpenAI API key from: https://platform.openai.com/api-keys")
        print("   - Weather tool uses mock data, so OpenWeather API key is optional")
    except Exception as e:
        print(f"❌ Error creating {env_path}: {e}")

if __name__ == "__main__":
    create_env_file()
